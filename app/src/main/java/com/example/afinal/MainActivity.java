package com.example.afinal;
import android.graphics.Bitmap.Config;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.view.Surface;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtProvider;
import ai.onnxruntime.OrtSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements JumpCounter.JumpListener, ArmCircleCounter.ArmCircleListener {
    private PreviewView previewView;
    private PoseOverlayView poseOverlay;
    private TextView jumpCountText;

    private TextView countdownText;
    private TextView cooldownTimer;
    private View countdownOverlay;
    private View leftIndicator;
    private View rightIndicator;
    
    // Congratulations screen elements
    private View congratsOverlay;
    private TextView congratsText;
    private TextView xpEarnedText;
    
    private OrtEnvironment env;
    private OrtSession session;
    private ExecutorService cameraExecutor;
    private ExecutorService inferenceExecutor;
    private JumpCounter jumpCounter;
    private ArmCircleCounter armCircleCounter;
    private Handler mainHandler;
    
    // Thread safety
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private volatile boolean isDestroyed = false;
    
    // Initialization state
    private volatile boolean modelReady = false;
    private volatile boolean cameraReady = false;
    
    // Exercise state management
    private boolean exerciseStarted = false;
    private boolean isCountingDown = true;
    private int countdownValue = 3;
    private boolean isInCooldown = false;
    private long cooldownStartTime = 0;
    private long exerciseStartTime = 0; // Track when exercise started
    private static final long COOLDOWN_DURATION_MS = 1000; // 1 second cooldown
    
    // Stats tracking
    private SharedPreferences userStats;
    private boolean exerciseCompleted = false;
    private static final int JUMPS_TO_COMPLETE = 20;
    private static final int ARM_CIRCLES_TO_COMPLETE = 20;
    private static final int XP_REWARD = 20;
    private int remainingJumps = 20; // Countdown from 20 to 0
    
    // Exercise type management
    private String exerciseType = "jump"; // Default to jump exercise
    private int remainingReps = 20; // Generic counter for any exercise
    
    // Constants for optimized processing
    private static final int MODEL_INPUT_SIZE = 320;
    private static final long INFERENCE_INTERVAL_MS = 8;
    private long lastInferenceTime = 0;
    
    // Memory management
    private static final int MAX_BITMAP_SIZE = 1024 * 1024;
    private final Object sessionLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            // Initialize handlers and executors
            mainHandler = new Handler(Looper.getMainLooper());
            cameraExecutor = Executors.newSingleThreadExecutor();
            inferenceExecutor = Executors.newSingleThreadExecutor();

            // Initialize SharedPreferences for stats
            userStats = getSharedPreferences("user_stats", MODE_PRIVATE);
            
            // Get exercise type from intent
            exerciseType = getIntent().getStringExtra("exercise_type");
            if (exerciseType == null) {
                exerciseType = "jump"; // Default to jump
            }
            
            // Set appropriate rep count based on exercise type
            if ("arm_circles".equals(exerciseType)) {
                remainingReps = ARM_CIRCLES_TO_COMPLETE;
            } else {
                remainingReps = JUMPS_TO_COMPLETE;
            }
            
            // Initialize all UI elements
            previewView = findViewById(R.id.previewView);
            poseOverlay = findViewById(R.id.poseOverlay);
            jumpCountText = findViewById(R.id.jumpCountText);

            countdownText = findViewById(R.id.countdownText);
            cooldownTimer = findViewById(R.id.cooldownTimer);
            countdownOverlay = findViewById(R.id.countdownOverlay);
            leftIndicator = findViewById(R.id.leftIndicator);
            rightIndicator = findViewById(R.id.rightIndicator);
            
            // Initialize congratulations elements
            congratsOverlay = findViewById(R.id.congratsOverlay);
            congratsText = findViewById(R.id.congratsText);
            xpEarnedText = findViewById(R.id.xpEarnedText);

            if (previewView == null || poseOverlay == null || jumpCountText == null) {
                throw new IllegalStateException("Failed to find required views");
            }
            
            // Initialize jump counter with this as the listener
            jumpCounter = new JumpCounter(this);
            // Initialize arm circle counter with this as the listener
            armCircleCounter = new ArmCircleCounter(this);
            updateCounterText();
            


            // Set immersive sticky mode for better fullscreen experience
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN);

            // Initialize camera and model FIRST, then start countdown
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                initModelAndCamera();
            } else {
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        initModelAndCamera();
                    } else {
                        Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }).launch(Manifest.permission.CAMERA);
            }
        } catch (Exception e) {
            Log.e("PoseTracker", "onCreate failed: " + e.getMessage(), e);
            Toast.makeText(this, "Initialization failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startCountdown() {
        isCountingDown = true;
        exerciseStarted = false;
        countdownValue = 3;
        
        // Show countdown overlay
        countdownOverlay.setVisibility(View.VISIBLE);
        countdownText.setVisibility(View.VISIBLE);
        
        // Disable jump detection during countdown
        if (jumpCounter != null) {
            jumpCounter.setJumpDetectionEnabled(false);
        }
        
        // Start countdown animation
        runCountdownTimer();
    }
    
    private void runCountdownTimer() {
        if (countdownValue > 0) {
            countdownText.setText(String.valueOf(countdownValue));
            
            // Animate countdown text
            countdownText.setScaleX(0.5f);
            countdownText.setScaleY(0.5f);
            countdownText.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(500)
                .withEndAction(() -> {
                    countdownText.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(500);
                });
            
            countdownValue--;
            mainHandler.postDelayed(this::runCountdownTimer, 1000);
        } else {
            // Countdown finished - start exercise
            startExercise();
        }
    }
    
    private void startExercise() {
        isCountingDown = false;
        exerciseStarted = true;
        exerciseStartTime = System.currentTimeMillis(); // Track exercise start time
        Log.d("PoseTracker", "Exercise started - isCountingDown set to false, exerciseStarted set to true");
        
        // Hide countdown elements
        countdownOverlay.setVisibility(View.GONE);
        countdownText.setVisibility(View.GONE);
        
        // Initialize visual indicators (no ready text needed)
        
        // Enable jump detection immediately - no need to wait for movement
        if (jumpCounter != null) {
            jumpCounter.setJumpDetectionEnabled(true);
            // Reset the jump counter to start fresh
            jumpCounter.reset();
        armCircleCounter.reset();
            // Reset remaining reps for countdown
            remainingReps = ("arm_circles".equals(exerciseType)) ? ARM_CIRCLES_TO_COMPLETE : JUMPS_TO_COMPLETE;
            updateCounterText();
        }
        

        
        // Start checking readiness state
        startReadinessChecker();
    }
    
    private void showJumpReady() {
        if (!isInCooldown && exerciseStarted) {
            // Show green indicators
            leftIndicator.setVisibility(View.VISIBLE);
            rightIndicator.setVisibility(View.VISIBLE);
            leftIndicator.setBackgroundColor(0xFF00FF00); // Green
            rightIndicator.setBackgroundColor(0xFF00FF00); // Green
            
            // Hide cooldown timer
            if (cooldownTimer != null) {
                cooldownTimer.setVisibility(View.GONE);
            }
        }
    }
    
    private void playJumpAnimation() {
        // Cool screen shake animation for successful jump
        runOnUiThread(() -> {
            if (poseOverlay != null && jumpCountText != null) {
                // Screen shake effect
                poseOverlay.animate()
                    .translationX(10f)
                    .setDuration(50)
                    .withEndAction(() -> {
                        poseOverlay.animate()
                            .translationX(-10f)
                            .setDuration(50)
                            .withEndAction(() -> {
                                poseOverlay.animate()
                                    .translationX(5f)
                                    .setDuration(50)
                                    .withEndAction(() -> {
                                        poseOverlay.animate()
                                            .translationX(0f)
                                            .setDuration(50);
                                    });
                            });
                    });
                
                // Jump counter bounce effect
                jumpCountText.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        jumpCountText.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100);
                    });
                
                // Brief green flash on side indicators
                leftIndicator.setBackgroundColor(0xFFFFFF00); // Bright yellow flash
                rightIndicator.setBackgroundColor(0xFFFFFF00);
                mainHandler.postDelayed(() -> {
                    if (!isInCooldown) {
                        leftIndicator.setBackgroundColor(0xFF00FF00); // Back to green
                        rightIndicator.setBackgroundColor(0xFF00FF00);
                    }
                }, 150);
            }
        });
    }
    
    private void showCooldown() {
        if (exerciseStarted) {
            isInCooldown = true;
            cooldownStartTime = System.currentTimeMillis();
            
            // Show red indicators
            leftIndicator.setVisibility(View.VISIBLE);
            rightIndicator.setVisibility(View.VISIBLE);
            leftIndicator.setBackgroundColor(0xFFFF0000); // Red
            rightIndicator.setBackgroundColor(0xFFFF0000); // Red
            
        
            // Show cooldown timer
            if (cooldownTimer != null) {
                cooldownTimer.setVisibility(View.VISIBLE);
            }
            
            // Start cooldown timer
            updateCooldownTimer();
        }
    }
    
    private void updateCooldownTimer() {
        if (isInCooldown && cooldownTimer != null) {
            long elapsed = System.currentTimeMillis() - cooldownStartTime;
            long remaining = COOLDOWN_DURATION_MS - elapsed;
            
            if (remaining > 0) {
                float seconds = remaining / 1000.0f;
                cooldownTimer.setText(String.format("Cooldown: %.1fs", seconds));
                mainHandler.postDelayed(this::updateCooldownTimer, 50); // Update every 50ms
            } else {
                // Cooldown finished
                isInCooldown = false;
                showJumpReady();
            }
        }
    }
    
    private void startReadinessChecker() {
        // Continuously check if we should show ready or cooldown state
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (exerciseStarted && !isDestroyed) {
                    if (!isInCooldown && jumpCounter != null) {
                        // Check if enough time has passed since last jump
                        long timeSinceLastJump = System.currentTimeMillis() - jumpCounter.getLastJumpTime();
                        if (timeSinceLastJump >= COOLDOWN_DURATION_MS) {
                            showJumpReady();
                        }
                    }
                    
                    // Schedule next check
                    mainHandler.postDelayed(this, 100);
                }
            }
        });
    }

    // JumpListener callback
    @Override
    public void onJumpDetected(int jumpCount) {
        // Only process jump detection if we're doing jump exercises
        if (!"jump".equals(exerciseType)) {
            return;
        }
        
        runOnUiThread(() -> {
            remainingReps = JUMPS_TO_COMPLETE - jumpCount;
            updateCounterText();
            
            // Play cool jump animation
            playJumpAnimation();
            
            // Start cooldown immediately after jump
            showCooldown();
        });
        
        // Track jumps in stats
        updateJumpStats(jumpCount);
        
        // Check if exercise is completed (when countdown reaches 0)
        if (!exerciseCompleted && remainingReps <= 0) {
            exerciseCompleted = true;
            completeExercise();
        }
    }
    
    // ArmCircleListener callback
    @Override
    public void onArmCircleDetected(int armCircleCount) {
        // Only process arm circle detection if we're doing arm circle exercises
        if (!"arm_circles".equals(exerciseType)) {
            return;
        }
        
        runOnUiThread(() -> {
            remainingReps = ARM_CIRCLES_TO_COMPLETE - armCircleCount;
            updateCounterText();
            
            // Play animation (can reuse jump animation or create specific arm circle animation)
            playJumpAnimation();
            
            // Start cooldown immediately after arm circle
            showCooldown();
        });
        
        // Track arm circles in stats (reuse jump stats for now)
        updateJumpStats(armCircleCount);
        
        // Check if exercise is completed (when countdown reaches 0)
        if (!exerciseCompleted && remainingReps <= 0) {
            exerciseCompleted = true;
            completeExercise();
        }
    }
    
    private void updateCounterText() {
        if ("arm_circles".equals(exerciseType)) {
            jumpCountText.setText("Arm Circles: " + remainingReps);
        } else {
            jumpCountText.setText("Jumps: " + remainingReps);
        }
    }
    
    private void updateJumpStats(int currentJumps) {
        // Update total jump count
        int totalJumps = userStats.getInt("jump_count", 0);
        
        // Only add the new jumps
        int prevJumps = userStats.getInt("current_exercise_jumps", 0);
        int newJumps = currentJumps - prevJumps;
        
        if (newJumps > 0) {
            totalJumps += newJumps;
            userStats.edit()
                .putInt("jump_count", totalJumps)
                .putInt("current_exercise_jumps", currentJumps)
                .apply();
        }
    }
    
    private void completeExercise() {
        android.util.Log.d("MainActivity", "Exercise completed! Updating database...");
        
        // Update stats via API (database)
        ApiService apiService = new ApiService();
        String userId = ApiService.getUserId(this);
        
        // Calculate session duration (in seconds)
        long sessionDuration = (System.currentTimeMillis() - exerciseStartTime) / 1000;
        
        android.util.Log.d("MainActivity", "Sending to API - User: " + userId + ", Jumps: " + JUMPS_TO_COMPLETE + ", XP: " + XP_REWARD + ", Duration: " + sessionDuration + "s");
        
        // Check if user ID is valid
        if (userId == null || userId.isEmpty()) {
            android.util.Log.e("MainActivity", "ERROR: User ID is null or empty! User may not be logged in.");
            android.widget.Toast.makeText(this, "Error: Please log in again", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        
        // Add timeout to API call and detailed logging
        android.util.Log.d("MainActivity", "🔄 Starting API call to update database...");
        
        apiService.updateUserStats(userId, JUMPS_TO_COMPLETE, XP_REWARD, (int) sessionDuration)
            .thenAccept(updatedStats -> {
                // Database update successful!
                runOnUiThread(() -> {
                    android.util.Log.d("MainActivity", "✅ DATABASE UPDATE SUCCESS! New stats - Level: " + updatedStats.level + ", XP: " + updatedStats.xp + ", Total Jumps: " + updatedStats.totalJumps);
                    android.widget.Toast.makeText(this, "✅ Database updated successfully!", android.widget.Toast.LENGTH_SHORT).show();
                    
                    // Update local cache with fresh database data
                    userStats.edit()
                        .putInt("xp", updatedStats.xp)
                        .putInt("level", updatedStats.level)
                        .putInt("jump_count", updatedStats.totalJumps)
                        .putInt("exercises_completed", updatedStats.exercisesCompleted)
                        .apply();
                    
                    // Database update successful - no need to show message
                    
                    // Show congratulations screen overlay
                    showCongratulationsScreen();
                });
            })
            .exceptionally(throwable -> {
                // Database update failed - use local fallback
                runOnUiThread(() -> {
                    android.util.Log.e("MainActivity", "DATABASE UPDATE FAILED! Error: " + throwable.getMessage(), throwable);
                    android.widget.Toast.makeText(this, "⚠️ Database update failed - check connection", android.widget.Toast.LENGTH_LONG).show();
                    
                    // Update local stats as fallback
        int currentXp = userStats.getInt("xp", 0);
                    int currentJumps = userStats.getInt("jump_count", 0);
        int exercisesCompleted = userStats.getInt("exercises_completed", 0);
        
        userStats.edit()
            .putInt("xp", currentXp + XP_REWARD)
                        .putInt("jump_count", currentJumps + JUMPS_TO_COMPLETE)
            .putInt("exercises_completed", exercisesCompleted + 1)
            .apply();
        
                    showCongratulationsScreen();
                });
                return null;
            });
    }
    
    private void showCongratulationsScreen() {
        runOnUiThread(() -> {
            // Show congratulations overlay and text
            congratsOverlay.setVisibility(View.VISIBLE);
            congratsText.setVisibility(View.VISIBLE);
            xpEarnedText.setVisibility(View.VISIBLE);
            
            // Update XP text with actual earned amount
            xpEarnedText.setText("+" + XP_REWARD + " XP EARNED!");
            
            // Make XP text tappable to proceed
            xpEarnedText.setOnClickListener(v -> {
                // User tapped XP text - proceed to main screen immediately
                proceedToMainScreen();
            });
            
            // Animate the congratulations text
            congratsText.setAlpha(0f);
            xpEarnedText.setAlpha(0f);
            
            congratsText.animate()
                .alpha(1f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(800)
                .withEndAction(() -> {
                    congratsText.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200);
                });
            
            xpEarnedText.animate()
                .alpha(1f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(600)
                .setStartDelay(400)
                .withEndAction(() -> {
                    xpEarnedText.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            // Add subtle hint that it's tappable after animation completes
                            xpEarnedText.animate()
                                .scaleX(1.05f)
                                .scaleY(1.05f)
                                .setDuration(500)
                                .withEndAction(() -> {
                                    xpEarnedText.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(500);
                                });
                        });
                });
            
            // Auto return to main screen if user doesn't tap (fallback)
            mainHandler.postDelayed(() -> {
                proceedToMainScreen();
            }, 5000); // 5 second fallback delay
        });
    }
    
    private void proceedToMainScreen() {
        Intent intent = new Intent(this, ExercisesActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        // Return to exercises activity
        Intent intent = new Intent(this, ExercisesActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void initModelAndCamera() {
        // Show loading message
        runOnUiThread(() -> {
            if (countdownText != null) {
                countdownText.setText("Loading...");
                countdownText.setVisibility(View.VISIBLE);
            }
        });
        
        // Initialize both model and camera, then start countdown when both are ready
        initModel();
        startCamera();
    }
    
    private void checkIfReadyToStart() {
        if (modelReady && cameraReady && !isDestroyed) {
            runOnUiThread(() -> {
                startCountdown();
            });
        }
    }

    private void initModel() {
        inferenceExecutor.execute(() -> {
        try {
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            
                // Enable optimization for mobile with better memory management
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
                options.setIntraOpNumThreads(2);
                options.setMemoryPatternOptimization(true);
                options.setCPUArenaAllocator(false); // Reduce memory usage
            
            byte[] modelBytes = loadModelFile();
            if (modelBytes == null || modelBytes.length == 0) {
                throw new IOException("Model file is empty or not found");
            }
            
                synchronized (sessionLock) {
            session = env.createSession(modelBytes, options);
                }
                
            Log.i("PoseTracker", "Model loaded successfully");
                modelReady = true;
                checkIfReadyToStart();
                
        } catch (Exception e) {
            Log.e("PoseTracker", "Model init failed: " + e.getMessage(), e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Failed to load model: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            });
        }
        });
    }

    private byte[] loadModelFile() throws IOException {
        // First try to load yolov8n-pose.onnx (smaller model for faster inference)
        byte[] modelBytes = tryLoadModelFile("yolov8n-pose.onnx");
        
        // If that fails, try other models
        if (modelBytes == null) {
            modelBytes = tryLoadModelFile("yolov8m-pose.onnx");
        }
        
        if (modelBytes == null) {
            throw new IOException("Failed to load any ONNX model file");
        }
        
        return modelBytes;
    }
    
    private byte[] tryLoadModelFile(String fileName) {
        Log.i("PoseTracker", "Trying to load model: " + fileName);
        try {
            InputStream inputStream = getAssets().open(fileName);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            inputStream.close();
            byte[] modelBytes = buffer.toByteArray();
            Log.i("PoseTracker", "Successfully loaded model: " + fileName);
            return modelBytes;
        } catch (IOException e) {
            Log.e("PoseTracker", "Failed to load model file " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Get the display rotation
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                
                // Optimized camera settings with better memory management
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(480, 640)) // Better aspect ratio for mobile
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setTargetRotation(rotation)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImage);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                Log.i("PoseTracker", "Camera started successfully");
                cameraReady = true;
                checkIfReadyToStart();
                
            } catch (Exception e) {
                Log.e("PoseTracker", "Camera setup failed: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Camera setup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImage(@NonNull ImageProxy imageProxy) {
        // Check if we're already processing or destroyed
        if (isDestroyed || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close();
            return;
        }

        // Remove rate limiting for immediate landmark display
            // Process every frame for maximum responsiveness
        long currentTime = System.currentTimeMillis();
        lastInferenceTime = currentTime;

        // Run inference on separate thread to avoid blocking camera
        inferenceExecutor.execute(() -> {
            try {
                synchronized (sessionLock) {
                    if (session == null || isDestroyed) {
                        return;
                    }

                    // Process with better error handling
            float[] inputData = preprocessImage(imageProxy);
            if (inputData == null) {
                return;
            }
            
                    OnnxTensor inputTensor = null;
            OrtSession.Result result = null;
            
            try {
                        inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), 
                                new long[]{1, 3, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE});
                        
                        // Run inference with timeout protection
                result = session.run(java.util.Collections.singletonMap("images", inputTensor));
                float[][][] output = (float[][][]) result.get(0).getValue();
                
                        // Parse keypoints with improved accuracy
                List<float[]> keypoints = parseKeypoints(output, imageProxy.getWidth(), imageProxy.getHeight());
                        
                        // Update UI on main thread
                        runOnUiThread(() -> {
                            if (!isDestroyed) {
                                // ALWAYS show landmarks - no conditions, no cooldown blocking
                poseOverlay.setKeypoints(keypoints);
                
                                // Only process keypoints for detection AFTER exercise starts (not during countdown)
                                if (keypoints.size() >= 17 && exerciseStarted) {
                                    // Use appropriate counter based on exercise type
                                    if ("arm_circles".equals(exerciseType)) {
                                        armCircleCounter.processKeypoints(keypoints);
                                    } else {
                                        jumpCounter.processKeypoints(keypoints);
                                    }
                                }
                            }
                        });
                        
            } finally {
                        // Clean up resources
                if (inputTensor != null) {
                            try {
                    inputTensor.close();
                            } catch (Exception e) {
                                Log.e("PoseTracker", "Error closing input tensor", e);
                            }
                }
                if (result != null) {
                            try {
                    result.close();
                            } catch (Exception e) {
                                Log.e("PoseTracker", "Error closing result", e);
                            }
                        }
                }
            }
        } catch (Exception e) {
            Log.e("PoseTracker", "Error in image processing", e);
        } finally {
                isProcessing.set(false);
            imageProxy.close();
        }
        });
    }
    


    private float[] preprocessImage(ImageProxy imageProxy) {
        try {
            Bitmap bitmap = toBitmap(imageProxy);
            if (bitmap == null) {
                return null;
            }
            
            // Get the device rotation and apply it
            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            if (rotation != 0) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(rotation);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                bitmap = rotatedBitmap;
            }
            
            // Resize to a smaller size for better performance
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, false);
            if (bitmap != resizedBitmap) {
                bitmap.recycle();
            }
            
            // Prepare arrays for processing
            int[] pixels = new int[MODEL_INPUT_SIZE * MODEL_INPUT_SIZE];
            float[] normalizedData = new float[3 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE];
            
            // Get all pixels at once
            resizedBitmap.getPixels(pixels, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE);
            resizedBitmap.recycle();
            
            // Process pixels efficiently
            int channelOffset = MODEL_INPUT_SIZE * MODEL_INPUT_SIZE;
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                normalizedData[i] = ((pixel >> 16) & 0xFF) / 255.0f;                  // Red channel
                normalizedData[i + channelOffset] = ((pixel >> 8) & 0xFF) / 255.0f;   // Green channel
                normalizedData[i + channelOffset * 2] = (pixel & 0xFF) / 255.0f;      // Blue channel
            }
            
            return normalizedData;
        } catch (Exception e) {
            Log.e("PoseTracker", "Preprocessing failed", e);
            return null;
        }
    }

    private Bitmap toBitmap(ImageProxy imageProxy) {
        try {
            if (imageProxy.getFormat() == ImageFormat.YUV_420_888) {
                ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
                if (planes.length < 3) {
                    return null;
                }
                
                ByteBuffer yBuffer = planes[0].getBuffer();
                ByteBuffer uBuffer = planes[1].getBuffer();
                ByteBuffer vBuffer = planes[2].getBuffer();

                int ySize = yBuffer.remaining();
                int uSize = uBuffer.remaining();
                int vSize = vBuffer.remaining();

                byte[] nv21 = new byte[ySize + uSize + vSize];
                yBuffer.get(nv21, 0, ySize);
                vBuffer.get(nv21, ySize, vSize);
                uBuffer.get(nv21, ySize + vSize, uSize);

                int width = imageProxy.getWidth();
                int height = imageProxy.getHeight();
                YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 75, out);
                byte[] imageBytes = out.toByteArray();
                return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            } else {
                Log.e("PoseTracker", "Unsupported image format: " + imageProxy.getFormat());
                return null;
            }
        } catch (Exception e) {
            Log.e("PoseTracker", "Bitmap conversion failed", e);
            return null;
        }
    }

    private List<float[]> parseKeypoints(float[][][] output, int imgWidth, int imgHeight) {
        List<float[]> keypoints = new ArrayList<>();
        try {
            if (output == null || output.length == 0 || output[0].length != 56) {
                return createEmptyKeypoints();
            }
            
            float[][] detections = output[0]; // Shape: [56, 8400]
            int numDetections = detections[0].length;
            
            if (numDetections == 0) {
                return createEmptyKeypoints();
            }
            
            // Find best detection based on confidence and area
            float bestScore = -1;
            int bestIdx = -1;
            
            for (int i = 0; i < numDetections; i++) {
                float score = detections[4][i];
                
                // Also consider bounding box area for better detection
                if (detections.length > 4) {
                    float x1 = detections[0][i];
                    float y1 = detections[1][i];
                    float x2 = detections[2][i];
                    float y2 = detections[3][i];
                    float area = Math.abs((x2 - x1) * (y2 - y1));
                    
                    // Prefer larger detections with reasonable confidence
                    float combinedScore = score * (1 + area * 0.001f);
                    
                    if (combinedScore > bestScore) {
                        bestScore = combinedScore;
                        bestIdx = i;
                    }
                } else if (score > bestScore) {
                    bestScore = score;
                    bestIdx = i;
                }
            }
            
            // Very low threshold to show landmarks immediately - let overlay handle low confidence
            if (bestIdx == -1 || detections[4][bestIdx] < 0.0001f) {
                return createEmptyKeypoints();
            }
            
            // Determine if we're in portrait mode based on actual image dimensions
            boolean isPortrait = imgHeight > imgWidth;
            
            // Extract keypoints for the best detection with correct coordinate mapping
            for (int k = 0; k < 17; k++) {
                int xIdx = 5 + (k * 3);
                int yIdx = 5 + (k * 3) + 1;
                int confIdx = 5 + (k * 3) + 2;
                
                if (xIdx < detections.length && yIdx < detections.length && confIdx < detections.length) {
                    float x = detections[xIdx][bestIdx];
                    float y = detections[yIdx][bestIdx];
                    float conf = detections[confIdx][bestIdx];
                    
                    // Normalize coordinates to 0-1 range
                    x = x / MODEL_INPUT_SIZE;
                    y = y / MODEL_INPUT_SIZE;
                    
                    // CAMERA IS MIRRORED - flip horizontally first
                    x = 1.0f - x;
                    
                    if (isPortrait) {
                        // For portrait mode, swap the axes after mirroring
                        float temp = x;
                        x = y;
                        y = temp;
                    }
                    
                    // Clamp values to ensure they're within valid range
                    x = Math.max(0, Math.min(1, x));
                    y = Math.max(0, Math.min(1, y));
                    
                    // Apply confidence smoothing for better stability
                    conf = Math.max(0, Math.min(1, conf));
                    
                    keypoints.add(new float[]{x, y, conf});
                } else {
                    keypoints.add(new float[]{0, 0, 0});
                }
            }
        } catch (Exception e) {
            Log.e("PoseTracker", "Error parsing keypoints", e);
            return createEmptyKeypoints();
        }
        
        return keypoints;
    }
    
    // Helper method to create empty keypoints list
    private List<float[]> createEmptyKeypoints() {
        List<float[]> emptyKeypoints = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            emptyKeypoints.add(new float[]{0, 0, 0});
        }
        return emptyKeypoints;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        
        // Shut down executors safely
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (inferenceExecutor != null) {
            inferenceExecutor.shutdown();
        }
        
        // Clean up ONNX resources
        if (inferenceExecutor != null) {
            inferenceExecutor.execute(() -> {
                synchronized (sessionLock) {
                    try {
                        if (session != null) {
                            session.close();
                            session = null;
                        }
                        if (env != null) {
                            env.close();
                            env = null;
                        }
        } catch (OrtException e) {
                        Log.e("PoseTracker", "Error closing ONNX resources", e);
                    }
                }
            });
        }
        
        // Clear references
        jumpCounter = null;
        poseOverlay = null;
        jumpCountText = null;

        countdownText = null;
        cooldownTimer = null;
        countdownOverlay = null;
        leftIndicator = null;
        rightIndicator = null;
        
        // Clear congratulations elements
        congratsOverlay = null;
        congratsText = null;
        xpEarnedText = null;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Reset processing state when paused
        isProcessing.set(false);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reset state when resuming
        if (jumpCounter != null) {
            // Don't reset the counter, just ensure it's ready
            Log.d("PoseTracker", "Activity resumed, current jumps: " + jumpCounter.getJumpCount());
        }
    }
}