package com.example.afinal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * JumpCounter - Ultra-responsive jump detection using frame-to-frame movement analysis
 */
public class JumpCounter {
    public interface JumpListener {
        void onJumpDetected(int jumpCount);
    }
    
    // Smart jump detection constants with validation
    private static final float MOVEMENT_THRESHOLD = 0.015f; // Balanced threshold to avoid false positives
    private static final float MIN_CONFIDENCE = 0.3f; // Higher confidence for reliable detection
    private static final long JUMP_COOLDOWN_MS = 1000; // 1 second cooldown between jumps
    private static final float MIN_JUMP_HEIGHT = 0.025f; // Minimum movement to qualify as jump
    private static final int VALIDATION_FRAMES = 2; // Frames to validate jump motion
    private static final String TAG = "JumpCounter";
    
    // Frame-to-frame tracking with validation
    private int jumpCount = 0;
    private float[] prevShoulderY = new float[2]; // Left and right shoulders
    private float[] prevHipY = new float[2]; // Left and right hips
    private boolean isInUpwardMotion = false;
    private long lastJumpTime = 0;
    private boolean hasValidPrevFrame = false;
    
    // Jump validation state
    private float[] recentMovements = new float[VALIDATION_FRAMES]; // Track recent movements
    private int movementIndex = 0;
    private float totalUpwardMovement = 0f;
    private int consecutiveUpwardFrames = 0;
    
    // Enhanced detection state
    private boolean jumpDetectionEnabled = true;
    
    // Callback
    private JumpListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public JumpCounter(JumpListener listener) {
        this.listener = listener;
    }
    
    /**
     * Ultra-fast jump detection using frame-to-frame movement analysis
     */
    public void processKeypoints(java.util.List<float[]> keypoints) {
        if (keypoints == null || keypoints.size() < 17 || !jumpDetectionEnabled) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Get key body parts for jump detection
        float[] leftShoulder = keypoints.get(5);   // More responsive than baseline
        float[] rightShoulder = keypoints.get(6);
        float[] leftHip = keypoints.get(11);       // Hips move more during jumps
        float[] rightHip = keypoints.get(12);
        
        // Check if we have enough confidence
        if (leftShoulder[2] < MIN_CONFIDENCE && rightShoulder[2] < MIN_CONFIDENCE &&
            leftHip[2] < MIN_CONFIDENCE && rightHip[2] < MIN_CONFIDENCE) {
            Log.d(TAG, "Not enough confidence for jump detection");
            return;
        }
        
        // Get current Y positions
        float[] currentShoulderY = {leftShoulder[1], rightShoulder[1]};
        float[] currentHipY = {leftHip[1], rightHip[1]};
            
        // Skip first frame (need previous frame for comparison)
        if (!hasValidPrevFrame) {
            prevShoulderY[0] = currentShoulderY[0];
            prevShoulderY[1] = currentShoulderY[1];
            prevHipY[0] = currentHipY[0];
            prevHipY[1] = currentHipY[1];
            hasValidPrevFrame = true;
            Log.d(TAG, "First frame recorded for jump detection");
            return;
        }
        
        // Calculate frame-to-frame movement (negative = upward)
        float shoulderMovement = 0;
        float hipMovement = 0;
        int validShoulders = 0;
        int validHips = 0;
        
        // Calculate average shoulder movement
        if (leftShoulder[2] > MIN_CONFIDENCE) {
            shoulderMovement += (currentShoulderY[0] - prevShoulderY[0]);
            validShoulders++;
        }
        if (rightShoulder[2] > MIN_CONFIDENCE) {
            shoulderMovement += (currentShoulderY[1] - prevShoulderY[1]);
            validShoulders++;
        }
        if (validShoulders > 0) {
            shoulderMovement /= validShoulders;
        }
        
        // Calculate average hip movement
        if (leftHip[2] > MIN_CONFIDENCE) {
            hipMovement += (currentHipY[0] - prevHipY[0]);
            validHips++;
        }
        if (rightHip[2] > MIN_CONFIDENCE) {
            hipMovement += (currentHipY[1] - prevHipY[1]);
            validHips++;
        }
        if (validHips > 0) {
            hipMovement /= validHips;
        }
        
        // Use the most significant movement (shoulders or hips)
        float totalMovement = 0;
        if (validShoulders > 0 && validHips > 0) {
            // Use weighted average (hips are more reliable for jumps)
            totalMovement = (shoulderMovement * 0.4f + hipMovement * 0.6f);
        } else if (validShoulders > 0) {
            totalMovement = shoulderMovement;
        } else if (validHips > 0) {
            totalMovement = hipMovement;
        }
        
        // Update movement history for validation
        recentMovements[movementIndex] = totalMovement;
        movementIndex = (movementIndex + 1) % VALIDATION_FRAMES;
        
        // Multi-criteria jump validation
        boolean isStrongUpwardMovement = totalMovement < -MOVEMENT_THRESHOLD;
        boolean hasGoodConfidence = (validShoulders > 0 || validHips > 0);
        boolean cooldownPassed = currentTime - lastJumpTime > JUMP_COOLDOWN_MS;
        
        Log.d(TAG, String.format("Movement: Total=%.4f, Strong=%s, Confidence=%s, Cooldown=%s", 
                totalMovement, isStrongUpwardMovement, hasGoodConfidence, cooldownPassed));
        
        if (isStrongUpwardMovement) {
            consecutiveUpwardFrames++;
            totalUpwardMovement += Math.abs(totalMovement);
            
            // SMART jump detection with multiple validation criteria
            if (consecutiveUpwardFrames >= VALIDATION_FRAMES && 
                totalUpwardMovement > MIN_JUMP_HEIGHT && 
                hasGoodConfidence && 
                cooldownPassed &&
                !isInUpwardMotion) {
                
                // Validated jump detected!
                jumpCount++;
                lastJumpTime = currentTime;
                isInUpwardMotion = true;
                
                Log.d(TAG, "✅ VALIDATED JUMP! Count: " + jumpCount + 
                      " (frames: " + consecutiveUpwardFrames + ", height: " + totalUpwardMovement + ")");
                
                // Notify listener
                if (listener != null) {
                    final int count = jumpCount;
                    mainHandler.post(() -> listener.onJumpDetected(count));
                }
                
                // Reset validation state
                resetValidationState();
            }
        } else {
            // Not upward movement - check if we should reset validation
            if (totalMovement > MOVEMENT_THRESHOLD * 0.5f) {
                // Downward movement - end of jump motion
                if (isInUpwardMotion) {
                    isInUpwardMotion = false;
                    Log.d(TAG, "Jump motion completed");
                }
                resetValidationState();
            }
        }
        
        // Update previous frame data
        prevShoulderY[0] = currentShoulderY[0];
        prevShoulderY[1] = currentShoulderY[1];
        prevHipY[0] = currentHipY[0];
        prevHipY[1] = currentHipY[1];
    }
    
    /**
     * Reset validation state for clean detection
     */
    private void resetValidationState() {
        consecutiveUpwardFrames = 0;
        totalUpwardMovement = 0f;
        for (int i = 0; i < VALIDATION_FRAMES; i++) {
            recentMovements[i] = 0f;
        }
    }
    
    /**
     * Reset the jump counter to zero
     */
    public void reset() {
        jumpCount = 0;
        isInUpwardMotion = false;
        hasValidPrevFrame = false;
        resetValidationState();
        
        Log.d(TAG, "Jump counter reset to 0");
        if (listener != null) {
            mainHandler.post(() -> listener.onJumpDetected(0));
        }
    }
    
    /**
     * Get the current jump count
     */
    public int getJumpCount() {
        return jumpCount;
    }
    
    /**
     * Enable or disable jump detection
     */
    public void setJumpDetectionEnabled(boolean enabled) {
        this.jumpDetectionEnabled = enabled;
        Log.d(TAG, "Jump detection " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if currently in upward motion
     */
    public boolean isInUpwardMotion() {
        return isInUpwardMotion;
    }
    
    /**
     * Get the quality of pose detection for jump counting
     */
    public float getDetectionQuality(java.util.List<float[]> keypoints) {
        if (keypoints == null || keypoints.size() < 13) {
            return 0f;
        }
        
        // Check confidence of key points for jump detection
        float[] leftShoulder = keypoints.get(5);
        float[] rightShoulder = keypoints.get(6);
        float[] leftHip = keypoints.get(11);
        float[] rightHip = keypoints.get(12);
        
        float totalConfidence = leftShoulder[2] + rightShoulder[2] + leftHip[2] + rightHip[2];
        return Math.min(1.0f, totalConfidence / 4.0f);
    }
    
    /**
     * Set movement sensitivity (lower = more sensitive)
     */
    public void setMovementSensitivity(float sensitivity) {
        // This could be used to adjust MOVEMENT_THRESHOLD if needed
        Log.d(TAG, "Movement sensitivity could be adjusted to: " + sensitivity);
    }
    
    /**
     * Get the time of the last detected jump
     */
    public long getLastJumpTime() {
        return lastJumpTime;
    }
} 