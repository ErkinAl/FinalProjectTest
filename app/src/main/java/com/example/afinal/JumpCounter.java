package com.example.afinal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * JumpCounter - Dedicated class for detecting and counting jump events
 * using pose landmark data.
 */
public class JumpCounter {
    // Interface for jump event callbacks
    public interface JumpListener {
        void onJumpDetected(int jumpCount);
    }
    
    // Constants for jump detection tuning
    private static final float SHOULDER_THRESHOLD = 0.020f; // Threshold for shoulder movement
    private static final float MIN_CONFIDENCE = 0.05f; // Minimum confidence for a valid keypoint
    private static final long JUMP_COOLDOWN_MS = 250; // Increased cooldown to prevent double counting
    private static final String TAG = "JumpCounter"; // Tag for logging
    
    // Jump tracking state
    private int jumpCount = 0;
    private float prevShoulderY = 0f;
    private long lastJumpTime = 0;
    private boolean isInUpwardMotion = false;
    
    // Callback
    private JumpListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public JumpCounter(JumpListener listener) {
        this.listener = listener;
    }
    
    /**
     * Process new keypoints to detect jumps
     * @param keypoints List of keypoints from pose detection [x, y, confidence]
     */
    public void processKeypoints(java.util.List<float[]> keypoints) {
        if (keypoints == null || keypoints.size() < 7) {
            return; // Not enough keypoints
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Get shoulder positions (keypoints 5 and 6 are left and right shoulders)
        float leftShoulderY = 0;
        float leftShoulderConf = 0;
        float rightShoulderY = 0;
        float rightShoulderConf = 0;
        
        if (keypoints.size() >= 7) {
            float[] leftShoulder = keypoints.get(5);
            float[] rightShoulder = keypoints.get(6);
            
            leftShoulderY = leftShoulder[1];
            leftShoulderConf = leftShoulder[2];
            rightShoulderY = rightShoulder[1];
            rightShoulderConf = rightShoulder[2];
            
            // Log raw shoulder data
            Log.d(TAG, String.format("Raw LEFT shoulder: Y=%.4f, Conf=%.4f", leftShoulderY, leftShoulderConf));
            Log.d(TAG, String.format("Raw RIGHT shoulder: Y=%.4f, Conf=%.4f", rightShoulderY, rightShoulderConf));
        }
        
        // Calculate weighted average of shoulder positions
        float shoulderY = 0;
        float totalWeight = 0;
        
        if (leftShoulderConf > MIN_CONFIDENCE) {
            shoulderY += leftShoulderY * leftShoulderConf;
            totalWeight += leftShoulderConf;
        }
        
        if (rightShoulderConf > MIN_CONFIDENCE) {
            shoulderY += rightShoulderY * rightShoulderConf;
            totalWeight += rightShoulderConf;
        }
        
        // Only process with enough confidence data
        if (totalWeight < MIN_CONFIDENCE) {
            Log.d(TAG, "Shoulder confidence too low - skipping frame");
            return;
        }
        
        // Normalize the position
        shoulderY /= totalWeight;
        
        // Log the weighted average shoulder position
        Log.d(TAG, String.format("AVERAGED shoulder Y: %.4f (weighted by confidence)", shoulderY));
        
        // Always update the previous position for reference
        if (prevShoulderY == 0) {
            prevShoulderY = shoulderY;
            Log.d(TAG, "First shoulder position recorded: " + shoulderY);
            return;
        }
        
        // Calculate the direction of movement
        // Negative diff means shoulders are moving UP (Y decreasing)
        float diff = shoulderY - prevShoulderY;
        
        // Log detailed movement data
        Log.d(TAG, String.format("SHOULDER MOVEMENT: Current=%.4f, Prev=%.4f, Diff=%.4f, UpMotion=%s", 
                shoulderY, prevShoulderY, diff, isInUpwardMotion));
        
        // State machine for jump detection
        // We're looking for an upward motion of shoulders (negative diff)
        if (!isInUpwardMotion && diff < -SHOULDER_THRESHOLD) {
            // Shoulders moving up - start of upward motion
            isInUpwardMotion = true;
            Log.d(TAG, "⬆️ UPWARD MOTION DETECTED - shoulders moving up by " + (-diff));
        } 
        // If we were in upward motion and now shoulders move down again,
        // consider it a completed jump (if sufficient time has passed)
        else if (isInUpwardMotion && diff > SHOULDER_THRESHOLD) {
            // Shoulders moving back down - end of jump
            isInUpwardMotion = false;
            
            if (currentTime - lastJumpTime > JUMP_COOLDOWN_MS) {
                jumpCount++;
                lastJumpTime = currentTime;
                Log.d(TAG, "🔄 JUMP COMPLETED! Count: " + jumpCount + " (shoulders moved down by " + diff + ")");
                
                // Notify listener on the main thread
                if (listener != null) {
                    final int count = jumpCount;
                    mainHandler.post(() -> listener.onJumpDetected(count));
                }
            } else {
                Log.d(TAG, "⏱️ Jump ignored - too soon after previous jump (cooldown active)");
            }
        }
        // Log small movements that don't trigger state changes
        else if (Math.abs(diff) > 0.005f) {
            if (diff < 0) {
                Log.d(TAG, "Small upward movement: " + (-diff) + " (below threshold)");
            } else {
                Log.d(TAG, "Small downward movement: " + diff + " (below threshold)");
            }
        }
        
        prevShoulderY = shoulderY;
    }
    
    /**
     * Reset the jump counter to zero
     */
    public void reset() {
        jumpCount = 0;
        isInUpwardMotion = false;
        prevShoulderY = 0f;
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
} 