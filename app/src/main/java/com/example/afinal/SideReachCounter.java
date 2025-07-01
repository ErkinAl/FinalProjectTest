package com.example.afinal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * SideReachCounter - Simple arm reach detection (no body lean required)
 */
public class SideReachCounter {
    public interface SideReachListener {
        void onSideReachDetected(int reachCount);
    }
    
    // Simple arm reach detection constants
    private static final float MIN_CONFIDENCE = 0.3f;
    private static final float ARM_REACH_THRESHOLD = 0.20f; // Much higher threshold to avoid false positives from natural arm positions
    private static final long REACH_COOLDOWN_MS = 1000; // Longer cooldown
    private static final String TAG = "SideReachCounter";
    
    // State tracking
    private int reachCount = 0;
    private long lastReachTime = 0;
    private boolean isInLeftReach = false;
    private boolean isInRightReach = false;
    
    // Callback
    private SideReachListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public SideReachCounter(SideReachListener listener) {
        this.listener = listener;
    }
    
    /**
     * Fixed arm reach detection - only counts positive extensions (arms going up/out)
     */
    public void processKeypoints(java.util.List<float[]> keypoints) {
        if (keypoints == null || keypoints.size() < 17) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Get arm keypoints
        float[] leftShoulder = keypoints.get(5);
        float[] rightShoulder = keypoints.get(6);
        float[] leftWrist = keypoints.get(9);
        float[] rightWrist = keypoints.get(10);
        
        // Check basic confidence
        if (leftShoulder[2] < MIN_CONFIDENCE || rightShoulder[2] < MIN_CONFIDENCE) {
            return;
        }
        
        // Calculate arm extensions - ONLY count positive values (arms going up/out)
        boolean leftArmReaching = false;
        boolean rightArmReaching = false;
        
        if (leftWrist[2] > MIN_CONFIDENCE) {
            // Left arm reaching: wrist extends left from shoulder OR wrist is above shoulder
            float leftHorizontalExtension = leftShoulder[0] - leftWrist[0]; // How far left (positive = extending left)
            float leftVerticalExtension = leftShoulder[1] - leftWrist[1]; // How far up (positive = extending up)
            
            // ONLY count if extensions are POSITIVE (actually reaching out/up, not hanging down)
            leftArmReaching = (leftHorizontalExtension > ARM_REACH_THRESHOLD && leftHorizontalExtension > 0) || 
                            (leftVerticalExtension > ARM_REACH_THRESHOLD && leftVerticalExtension > 0);
        }
        
        if (rightWrist[2] > MIN_CONFIDENCE) {
            // Right arm reaching: wrist extends right from shoulder OR wrist is above shoulder  
            float rightHorizontalExtension = rightWrist[0] - rightShoulder[0]; // How far right (positive = extending right)
            float rightVerticalExtension = rightShoulder[1] - rightWrist[1]; // How far up (positive = extending up)
            
            // ONLY count if extensions are POSITIVE (actually reaching out/up, not hanging down)
            rightArmReaching = (rightHorizontalExtension > ARM_REACH_THRESHOLD && rightHorizontalExtension > 0) || 
                             (rightVerticalExtension > ARM_REACH_THRESHOLD && rightVerticalExtension > 0);
        }
        
        // Cooldown check
        boolean cooldownPassed = currentTime - lastReachTime > REACH_COOLDOWN_MS;
        
        // Debug logging to see what's happening
        if (leftWrist[2] > MIN_CONFIDENCE && rightWrist[2] > MIN_CONFIDENCE) {
            float leftH = leftShoulder[0] - leftWrist[0];
            float leftV = leftShoulder[1] - leftWrist[1];
            float rightH = rightWrist[0] - rightShoulder[0];
            float rightV = rightShoulder[1] - rightWrist[1];
            
            Log.d(TAG, String.format("L_H:%.3f L_V:%.3f R_H:%.3f R_V:%.3f | LReach:%s RReach:%s | Cool:%s", 
                  leftH, leftV, rightH, rightV, leftArmReaching, rightArmReaching, cooldownPassed));
        }
        
        // Detect LEFT ARM REACH (allow both arms to be detected separately)
        if (leftArmReaching && !isInLeftReach && cooldownPassed) {
            isInLeftReach = true;
            reachCount++;
            lastReachTime = currentTime;
            
            Log.d(TAG, "✅ LEFT ARM REACH DETECTED! Count: " + reachCount);
            
            if (listener != null) {
                final int count = reachCount;
                mainHandler.post(() -> listener.onSideReachDetected(count));
            }
        }
        
        // Detect RIGHT ARM REACH (separate check, not else-if)
        if (rightArmReaching && !isInRightReach && cooldownPassed) {
            isInRightReach = true;
            reachCount++;
            lastReachTime = currentTime;
            
            Log.d(TAG, "✅ RIGHT ARM REACH DETECTED! Count: " + reachCount);
            
            if (listener != null) {
                final int count = reachCount;
                mainHandler.post(() -> listener.onSideReachDetected(count));
            }
        }
        
        // Reset reach state when arms return to normal position
        if (!leftArmReaching) {
            isInLeftReach = false;
        }
        if (!rightArmReaching) {
            isInRightReach = false;
        }
    }
    
    /**
     * Reset the side reach counter
     */
    public void reset() {
        reachCount = 0;
        isInLeftReach = false;
        isInRightReach = false;
        lastReachTime = 0;
        Log.d(TAG, "Side reach counter reset");
    }
    
    /**
     * Get current reach count
     */
    public int getReachCount() {
        return reachCount;
    }
    
    /**
     * Get current state for debugging
     */
    public String getCurrentState() {
        if (isInLeftReach && isInRightReach) return "BOTH_REACH";
        if (isInLeftReach) return "LEFT_REACH";
        if (isInRightReach) return "RIGHT_REACH";
        return "CENTER";
    }
} 