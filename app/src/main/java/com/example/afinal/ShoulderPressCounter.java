package com.example.afinal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * ShoulderPressCounter - Detects shoulder press movements by tracking wrist movement from shoulder level to above head
 */
public class ShoulderPressCounter {
    public interface ShoulderPressListener {
        void onShoulderPressDetected(int pressCount);
    }
    
    // Shoulder press detection constants
    private static final float MIN_CONFIDENCE = 0.3f;
    private static final float STARTING_POSITION_THRESHOLD = 0.12f; // Wrist close to shoulder level (starting position) - more forgiving
    private static final float PRESS_UP_THRESHOLD = 0.18f; // Wrist significantly above shoulder level (counts as 1 rep) - more sensitive
    private static final long SHOULDER_PRESS_COOLDOWN_MS = 800; // Cooldown between presses - faster reset
    private static final String TAG = "ShoulderPressCounter";
    
    // State tracking
    private int shoulderPressCount = 0;
    private long lastShoulderPressTime = 0;
    private boolean isInStartingPosition = false; // Start false, detect when user gets into position
    private boolean hasValidFrame = false;
    
    // Baseline positions for starting detection
    private float baselineLeftShoulderY = 0f;
    private float baselineRightShoulderY = 0f;
    
    // Callback
    private ShoulderPressListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public ShoulderPressCounter(ShoulderPressListener listener) {
        this.listener = listener;
    }
    
    /**
     * Detects shoulder presses by tracking absolute wrist position relative to shoulder:
     * 1. Detect starting position: Wrist at same level as shoulder (elbow and shoulder straight)
     * 2. Detect press: Wrist moves significantly UP from shoulder level - counts as 1 rep
     * 3. Reset: Wait for return to starting position before next rep can be counted
     */
    public void processKeypoints(java.util.List<float[]> keypoints) {
        if (keypoints == null || keypoints.size() < 17) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Get arm and shoulder keypoints
        float[] leftShoulder = keypoints.get(5);
        float[] rightShoulder = keypoints.get(6);
        float[] leftWrist = keypoints.get(9);
        float[] rightWrist = keypoints.get(10);
        
        // Check basic confidence for at least one arm
        boolean leftArmValid = leftShoulder[2] > MIN_CONFIDENCE && leftWrist[2] > MIN_CONFIDENCE;
        boolean rightArmValid = rightShoulder[2] > MIN_CONFIDENCE && rightWrist[2] > MIN_CONFIDENCE;
        
        if (!leftArmValid && !rightArmValid) {
            return;
        }
        
        // Establish baseline shoulder positions on first valid frame
        if (!hasValidFrame) {
            if (leftArmValid) {
                baselineLeftShoulderY = leftShoulder[1];
            }
            if (rightArmValid) {
                baselineRightShoulderY = rightShoulder[1];
            }
            hasValidFrame = true;
            return;
        }
        
        // Cooldown check
        boolean cooldownPassed = currentTime - lastShoulderPressTime > SHOULDER_PRESS_COOLDOWN_MS;
        
        // Check for shoulder press movement (either arm)
        boolean pressDetected = false;
        String armUsed = "";
        
        // Check left arm
        if (leftArmValid) {
            float leftWristRelativeToShoulder = leftShoulder[1] - leftWrist[1]; // Positive = wrist above shoulder
            
            // Check if in starting position (wrist close to shoulder level)
            if (Math.abs(leftWristRelativeToShoulder) <= STARTING_POSITION_THRESHOLD) {
                if (!isInStartingPosition) {
                    isInStartingPosition = true;
                    Log.d(TAG, "Left arm in STARTING POSITION: wrist-shoulder=" + leftWristRelativeToShoulder);
                }
            }
            // Check if pressing up (wrist significantly above shoulder) - COUNT AS 1 REP
            else if (leftWristRelativeToShoulder > PRESS_UP_THRESHOLD && isInStartingPosition) {
                pressDetected = true;
                armUsed = "LEFT";
                isInStartingPosition = false; // No longer in starting position
                Log.d(TAG, "✅ LEFT ARM SHOULDER PRESS! wrist-shoulder=" + leftWristRelativeToShoulder);
            }
        }
        
        // Check right arm (if left arm didn't detect press)
        if (!pressDetected && rightArmValid) {
            float rightWristRelativeToShoulder = rightShoulder[1] - rightWrist[1]; // Positive = wrist above shoulder
            
            // Check if in starting position (wrist close to shoulder level)
            if (Math.abs(rightWristRelativeToShoulder) <= STARTING_POSITION_THRESHOLD) {
                if (!isInStartingPosition) {
                    isInStartingPosition = true;
                    Log.d(TAG, "Right arm in STARTING POSITION: wrist-shoulder=" + rightWristRelativeToShoulder);
                }
            }
            // Check if pressing up (wrist significantly above shoulder) - COUNT AS 1 REP
            else if (rightWristRelativeToShoulder > PRESS_UP_THRESHOLD && isInStartingPosition) {
                pressDetected = true;
                armUsed = "RIGHT";
                isInStartingPosition = false; // No longer in starting position
                Log.d(TAG, "✅ RIGHT ARM SHOULDER PRESS! wrist-shoulder=" + rightWristRelativeToShoulder);
            }
        }
        
        // Debug logging with position values
        if (leftArmValid || rightArmValid) {
            float leftPos = leftArmValid ? (leftShoulder[1] - leftWrist[1]) : 0f;
            float rightPos = rightArmValid ? (rightShoulder[1] - rightWrist[1]) : 0f;
            Log.d(TAG, String.format("Starting:%s Detected:%s Arm:%s Cool:%s L:%.3f R:%.3f", 
                  isInStartingPosition, pressDetected, armUsed, cooldownPassed, leftPos, rightPos));
        }
        
        // Count the shoulder press
        if (pressDetected && cooldownPassed) {
            shoulderPressCount++;
            lastShoulderPressTime = currentTime;
            
            Log.d(TAG, "✅ SHOULDER PRESS DETECTED! Count: " + shoulderPressCount + " (Arm: " + armUsed + ")");
            
            if (listener != null) {
                final int count = shoulderPressCount;
                mainHandler.post(() -> listener.onShoulderPressDetected(count));
            }
        }
        
        // Update baseline shoulder positions occasionally (to handle camera movement)
        if (leftArmValid && System.currentTimeMillis() % 100 == 0) {
            baselineLeftShoulderY = leftShoulder[1];
        }
        if (rightArmValid && System.currentTimeMillis() % 100 == 0) {
            baselineRightShoulderY = rightShoulder[1];
        }
    }
    
    /**
     * Reset the shoulder press counter
     */
    public void reset() {
        shoulderPressCount = 0;
        isInStartingPosition = false; // Start false, need to detect starting position
        lastShoulderPressTime = 0;
        hasValidFrame = false;
        baselineLeftShoulderY = 0f;
        baselineRightShoulderY = 0f;
        Log.d(TAG, "Shoulder press counter reset");
    }
    
    /**
     * Get current shoulder press count
     */
    public int getShoulderPressCount() {
        return shoulderPressCount;
    }
    
    /**
     * Get current state for debugging
     */
    public String getCurrentState() {
        return isInStartingPosition ? "STARTING_POSITION" : "PRESSED_UP";
    }
} 