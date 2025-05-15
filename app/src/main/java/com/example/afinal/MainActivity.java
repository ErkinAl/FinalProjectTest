package com.example.afinal;
import android.graphics.Bitmap.Config;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.view.Surface;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
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

public class MainActivity extends AppCompatActivity implements JumpCounter.JumpListener {
    private PreviewView previewView;
    private PoseOverlayView poseOverlay;
    private TextView jumpCountText;
    private OrtEnvironment env;
    private OrtSession session;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private JumpCounter jumpCounter;
    
    // Constants for optimized processing
    private static final int MODEL_INPUT_SIZE = 320; // Smaller model input size for faster processing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            previewView = findViewById(R.id.previewView);
            poseOverlay = findViewById(R.id.poseOverlay);
            jumpCountText = findViewById(R.id.jumpCountText);

            if (previewView == null || poseOverlay == null || jumpCountText == null) {
                throw new IllegalStateException("Failed to find required views");
            }
            
            // Initialize jump counter with this as the listener
            jumpCounter = new JumpCounter(this);
            jumpCountText.setText("Jumps: 0");

            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    initModel();
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }).launch(Manifest.permission.CAMERA);
        } catch (Exception e) {
            Log.e("PoseTracker", "onCreate failed: " + e.getMessage(), e);
            Toast.makeText(this, "Initialization failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // JumpListener callback
    @Override
    public void onJumpDetected(int jumpCount) {
        jumpCountText.setText("Jumps: " + jumpCount);
    }

    private void initModel() {
        try {
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            
            // Enable optimization for mobile
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            options.setIntraOpNumThreads(2); // Use 2 threads for faster processing
            
            byte[] modelBytes = loadModelFile();
            if (modelBytes == null || modelBytes.length == 0) {
                throw new IOException("Model file is empty or not found");
            }
            
            session = env.createSession(modelBytes, options);
            Log.i("PoseTracker", "Model loaded successfully");
        } catch (Exception e) {
            Log.e("PoseTracker", "Model init failed: " + e.getMessage(), e);
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "Failed to load model: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            });
        }
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

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Get the display rotation
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                
                // High-performance camera settings for fast capture
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setTargetRotation(rotation)
                        .build();

                imageAnalysis.setAnalyzer(executor, this::processImage);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                Log.i("PoseTracker", "Camera started successfully");
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
        if (session == null) {
            imageProxy.close();
            return;
        }

        try {
            // Process every frame for maximum responsiveness
            float[] inputData = preprocessImage(imageProxy);
            if (inputData == null) {
                imageProxy.close();
                return;
            }
            
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), 
                    new long[]{1, 3, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE});
            OrtSession.Result result = null;
            
            try {
                // Run inference with minimal overhead
                result = session.run(java.util.Collections.singletonMap("images", inputTensor));
                float[][][] output = (float[][][]) result.get(0).getValue();
                
                // Parse keypoints and update UI
                List<float[]> keypoints = parseKeypoints(output, imageProxy.getWidth(), imageProxy.getHeight());
                poseOverlay.setKeypoints(keypoints);
                
                // Process keypoints for jump detection (if keypoints are valid)
                if (keypoints.size() >= 17) {
                    jumpCounter.processKeypoints(keypoints);
                }
            } finally {
                if (inputTensor != null) {
                    inputTensor.close();
                }
                if (result != null) {
                    result.close();
                }
            }
        } catch (Exception e) {
            Log.e("PoseTracker", "Error in image processing", e);
        } finally {
            imageProxy.close();
        }
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
            
            // Find best detection based on confidence
            float bestScore = -1;
            int bestIdx = -1;
            
            for (int i = 0; i < numDetections; i++) {
                float score = detections[4][i];
                if (score > bestScore) {
                    bestScore = score;
                    bestIdx = i;
                }
            }
            
            // Use a very low threshold to get landmarks even with low confidence
            if (bestIdx == -1 || bestScore < 0.00005f) {
                return createEmptyKeypoints();
            }
            
            // Determine if we're in portrait mode
            boolean isPortrait = imgHeight > imgWidth;
            
            // Extract keypoints for the best detection
            // Keypoints start at index 5, with 3 values (x,y,confidence) per keypoint
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
                    
                    // CAMERA IS MIRRORED - flip horizontally
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
        executor.shutdown();
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (OrtException e) {
            Log.e("PoseTracker", "Error closing session", e);
        }
    }
}