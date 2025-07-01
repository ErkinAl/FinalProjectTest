package com.example.afinal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * BicepsCurlCounter - Detects biceps curls by tracking wrist movement relative to elbow
 */
public class BicepsCurlCounter {
    public interface BicepsCurlListener {
        void onBicepsCurlDetected(int curlCount);
    }
    
    // Biceps curl detection constants
    private static final float MIN_CONFIDENCE = 0.3f;
    private static final float CURL_UP_THRESHOLD = 0.08f; // Wrist moves up relative to elbow
    private static final float CURL_DOWN_THRESHOLD = 0.05f; // Wrist moves back down
    private static final long BICEPS_CURL_COOLDOWN_MS = 800; // Cooldown between curls
    private static final String TAG = "BicepsCurlCounter";
    
    // State tracking
    private int bicepsCurlCount = 0;
    private long lastBicepsCurlTime = 0;
    private boolean isInCurlUpPosition = false; // Tracking if arm is in "up" position
    private boolean hasValidPrevFrame = false;
    
    // Previous frame data for movement detection
    private float prevLeftWristY = 0f;
    private float prevRightWristY = 0f;
    private float prevLeftElbowY = 0f;
    private float prevRightElbowY = 0f;
    
    // Callback
    private BicepsCurlListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public BicepsCurlCounter(BicepsCurlListener listener) {
        this.listener = listener;
    }
    
    /**
     * Detects biceps curls by tracking wrist movement relative to elbow:
     * 1. Wrist moves up significantly relative to elbow (curl up)
     * 2. Wrist returns down relative to elbow (curl down) - completes one rep
     */
    public void processKeypoints(java.util.List<float[]> keypoints) {
        if (keypoints == null || keypoints.size() < 17) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Get arm keypoints
        float[] leftElbow = keypoints.get(7);
        float[] rightElbow = keypoints.get(8);
        float[] leftWrist = keypoints.get(9);
        float[] rightWrist = keypoints.get(10);
        
        // Check basic confidence for at least one arm
        boolean leftArmValid = leftElbow[2] > MIN_CONFIDENCE && leftWrist[2] > MIN_CONFIDENCE;
        boolean rightArmValid = rightElbow[2] > MIN_CONFIDENCE && rightWrist[2] > MIN_CONFIDENCE;
        
        if (!leftArmValid && !rightArmValid) {
            return;
        }
        
        // Skip first frame (need previous frame for comparison)
        if (!hasValidPrevFrame) {
            if (leftArmValid) {
                prevLeftWristY = leftWrist[1];
                prevLeftElbowY = leftElbow[1];
            }
            if (rightArmValid) {
                prevRightWristY = rightWrist[1];
                prevRightElbowY = rightElbow[1];
            }
            hasValidPrevFrame = true;
            return;
        }
        
        // Cooldown check
        boolean cooldownPassed = currentTime - lastBicepsCurlTime > BICEPS_CURL_COOLDOWN_MS;
        
        // Check for biceps curl movement (either arm)
        boolean curlDetected = false;
        String armUsed = "";
        
        // Check left arm curl
        if (leftArmValid) {
            float leftWristRelativeToElbow = leftElbow[1] - leftWrist[1]; // Positive = wrist above elbow
            float prevLeftWristRelativeToElbow = prevLeftElbowY - prevLeftWristY;
            float leftCurlMovement = leftWristRelativeToElbow - prevLeftWristRelativeToElbow;
            
            // Detect curl up (wrist moves significantly up relative to elbow)
            if (!isInCurlUpPosition && leftCurlMovement > CURL_UP_THRESHOLD) {
                isInCurlUpPosition = true;
                Log.d(TAG, "Left arm curl UP detected: " + leftCurlMovement);
            }
            // Detect curl down (complete the rep)
            else if (isInCurlUpPosition && leftCurlMovement < -CURL_DOWN_THRESHOLD) {
                curlDetected = true;
                armUsed = "LEFT";
                isInCurlUpPosition = false;
            }
        }
        
        // Check right arm curl (if left arm didn't detect)
        if (!curlDetected && rightArmValid) {
            float rightWristRelativeToElbow = rightElbow[1] - rightWrist[1]; // Positive = wrist above elbow
            float prevRightWristRelativeToElbow = prevRightElbowY - prevRightWristY;
            float rightCurlMovement = rightWristRelativeToElbow - prevRightWristRelativeToElbow;
            
            // Detect curl up (wrist moves significantly up relative to elbow)
            if (!isInCurlUpPosition && rightCurlMovement > CURL_UP_THRESHOLD) {
                isInCurlUpPosition = true;
                Log.d(TAG, "Right arm curl UP detected: " + rightCurlMovement);
            }
            // Detect curl down (complete the rep)
            else if (isInCurlUpPosition && rightCurlMovement < -CURL_DOWN_THRESHOLD) {
                curlDetected = true;
                armUsed = "RIGHT";
                isInCurlUpPosition = false;
            }
        }
        
        // Debug logging
        if (leftArmValid || rightArmValid) {
            Log.d(TAG, String.format("CurlUp:%s Detected:%s Arm:%s Cool:%s", 
                  isInCurlUpPosition, curlDetected, armUsed, cooldownPassed));
        }
        
        // Count the biceps curl
        if (curlDetected && cooldownPassed) {
            bicepsCurlCount++;
            lastBicepsCurlTime = currentTime;
            
            Log.d(TAG, "✅ BICEPS CURL DETECTED! Count: " + bicepsCurlCount + " (Arm: " + armUsed + ")");
            
            if (listener != null) {
                final int count = bicepsCurlCount;
                mainHandler.post(() -> listener.onBicepsCurlDetected(count));
            }
        }
        
        // Update previous frame data
        if (leftArmValid) {
            prevLeftWristY = leftWrist[1];
            prevLeftElbowY = leftElbow[1];
        }
        if (rightArmValid) {
            prevRightWristY = rightWrist[1];
            prevRightElbowY = rightElbow[1];
        }
    }
    
    /**
     * Reset the biceps curl counter
     */
    public void reset() {
        bicepsCurlCount = 0;
        isInCurlUpPosition = false;
        lastBicepsCurlTime = 0;
        hasValidPrevFrame = false;
        prevLeftWristY = 0f;
        prevRightWristY = 0f;
        prevLeftElbowY = 0f;
        prevRightElbowY = 0f;
        Log.d(TAG, "Biceps curl counter reset");
    }
    
    /**
     * Get current biceps curl count
     */
    public int getBicepsCurlCount() {
        return bicepsCurlCount;
    }
    
    /**
     * Get current state for debugging
     */
    public String getCurrentState() {
        return isInCurlUpPosition ? "CURL_UP" : "READY";
    }
} 