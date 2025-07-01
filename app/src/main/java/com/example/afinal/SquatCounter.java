package com.example.afinal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * SquatCounter - Detects squat movements by tracking hip and shoulder positions going down and up
 */
public class SquatCounter {
    public interface SquatListener {
        void onSquatDetected(int squatCount);
    }
    
    // Squat detection constants
    private static final float MIN_CONFIDENCE = 0.3f;
    private static final float SQUAT_DOWN_THRESHOLD = 0.08f; // Hip moves down significantly from baseline
    private static final float SQUAT_UP_THRESHOLD = 0.06f; // Hip returns up close to baseline (completes rep)
    private static final long SQUAT_COOLDOWN_MS = 1000; // Cooldown between squats
    private static final String TAG = "SquatCounter";
    
    // State tracking
    private int squatCount = 0;
    private long lastSquatTime = 0;
    private boolean isInSquatDownPosition = false; // Tracking if in squat down position
    private boolean hasValidFrame = false;
    
    // Baseline position for squat detection (shoulder Y position)
    private float baselineShoulderY = 0f;
    
    // Callback
    private SquatListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public SquatCounter(SquatListener listener) {
        this.listener = listener;
    }
    
    /**
     * Detects squats by tracking shoulder Y movement:
     * 1. Establish baseline standing position (shoulder level)
     * 2. Detect squat down: Shoulders drop significantly from baseline
     * 3. Detect squat up: Shoulders return close to baseline - completes one rep
     */
    public void processKeypoints(java.util.List<float[]> keypoints) {
        if (keypoints == null || keypoints.size() < 17) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Get shoulder keypoints for squat detection (shoulders are most reliable)
        float[] leftShoulder = keypoints.get(5);
        float[] rightShoulder = keypoints.get(6);
        
        // Check if we have valid shoulder landmarks
        boolean shouldersValid = leftShoulder[2] > MIN_CONFIDENCE && rightShoulder[2] > MIN_CONFIDENCE;
        
        if (!shouldersValid) {
            return;
        }
        
        // Calculate average shoulder Y position
        float currentShoulderY = (leftShoulder[1] + rightShoulder[1]) / 2.0f;
        
        // Establish baseline shoulder position on first valid frame
        if (!hasValidFrame) {
            baselineShoulderY = currentShoulderY;
            hasValidFrame = true;
            Log.d(TAG, "Baseline established - Shoulder Y: " + baselineShoulderY);
            return;
        }
        
        // Cooldown check
        boolean cooldownPassed = currentTime - lastSquatTime > SQUAT_COOLDOWN_MS;
        
        // Calculate shoulder movement relative to baseline
        float shoulderMovement = currentShoulderY - baselineShoulderY; // Positive = moved down from baseline
        
        // Use shoulder Y movement as primary indicator for squat detection
        boolean squatDetected = false;
        
        // Detect squat down phase (shoulders drop significantly from baseline)
        if (!isInSquatDownPosition && shoulderMovement > SQUAT_DOWN_THRESHOLD) {
            isInSquatDownPosition = true;
            Log.d(TAG, "Squat DOWN detected - Shoulder movement: " + shoulderMovement);
        }
        // Detect squat up phase (shoulders return close to baseline) - COMPLETE REP
        else if (isInSquatDownPosition && Math.abs(shoulderMovement) < SQUAT_UP_THRESHOLD) {
            squatDetected = true;
            isInSquatDownPosition = false;
            Log.d(TAG, "✅ SQUAT UP detected - Shoulder movement: " + shoulderMovement);
        }
        
        // Debug logging with position values
        Log.d(TAG, String.format("Down:%s Detected:%s Cool:%s Shoulder:%.3f (baseline:%.3f)", 
              isInSquatDownPosition, squatDetected, cooldownPassed, shoulderMovement, baselineShoulderY));
        
        // Count the squat
        if (squatDetected && cooldownPassed) {
            squatCount++;
            lastSquatTime = currentTime;
            
            Log.d(TAG, "✅ SQUAT COMPLETED! Count: " + squatCount);
            
            if (listener != null) {
                final int count = squatCount;
                mainHandler.post(() -> listener.onSquatDetected(count));
            }
        }
        
        // Update baseline occasionally to handle camera movement (only when standing)
        if (System.currentTimeMillis() % 300 == 0) {
            // Gradually adjust baseline if user is standing (not in squat)
            if (!isInSquatDownPosition && Math.abs(shoulderMovement) < SQUAT_UP_THRESHOLD) {
                // Slowly adjust baseline to current position to account for camera movement
                baselineShoulderY = (baselineShoulderY * 0.9f) + (currentShoulderY * 0.1f);
            }
        }
    }
    
    /**
     * Reset the squat counter
     */
    public void reset() {
        squatCount = 0;
        isInSquatDownPosition = false;
        lastSquatTime = 0;
        hasValidFrame = false;
        baselineShoulderY = 0f;
        Log.d(TAG, "Squat counter reset");
    }
    
    /**
     * Get current squat count
     */
    public int getSquatCount() {
        return squatCount;
    }
    
    /**
     * Get current state for debugging
     */
    public String getCurrentState() {
        return isInSquatDownPosition ? "SQUAT_DOWN" : "STANDING";
    }
} 