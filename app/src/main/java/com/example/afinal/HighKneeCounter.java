package com.example.afinal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.List;

/**
 * HighKneeCounter - Detects high knee exercises by tracking knee lifting above hip level
 * Counts alternating knee lifts with no cooldown for rapid movement
 */
public class HighKneeCounter {
    public interface HighKneeListener {
        void onHighKneeDetected(int highKneeCount);
    }
    
    // High knee detection constants - Simple upward movement detection
    private static final float MIN_CONFIDENCE = 0.25f; // Confidence threshold for keypoints
    private static final float UPWARD_MOVEMENT_THRESHOLD = 0.04f; // How much knee must move up from baseline
    private static final float DOWN_MOVEMENT_THRESHOLD = 0.02f; // How much knee must come down to reset
    private static final long COOLDOWN_MS = 400; // 400ms cooldown between counts for same leg
    private static final int BASELINE_FRAMES = 10; // Frames to establish baseline
    private static final String TAG = "HighKneeCounter";
    
    // Tracking state
    private int highKneeCount = 0;
    private boolean hasValidPrevFrame = false;
    
    // Track each leg independently for alternating movement
    private KneeTracker leftKneeTracker = new KneeTracker("LEFT");
    private KneeTracker rightKneeTracker = new KneeTracker("RIGHT");
    
    // Callback
    private HighKneeListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public HighKneeCounter(HighKneeListener listener) {
        this.listener = listener;
    }
    
    /**
     * Process keypoints to detect high knee movements
     * Tracks when knees are lifted significantly above hip level
     */
    public void processKeypoints(List<float[]> keypoints) {
        if (keypoints == null || keypoints.size() < 17) {
            return;
        }
        
        // Get hip and knee keypoints
        float[] leftHip = keypoints.get(11);    // Left hip
        float[] rightHip = keypoints.get(12);   // Right hip
        float[] leftKnee = keypoints.get(13);   // Left knee
        float[] rightKnee = keypoints.get(14);  // Right knee
        
        // Check if we have enough confidence in the required keypoints
        boolean leftLegValid = leftHip[2] > MIN_CONFIDENCE && leftKnee[2] > MIN_CONFIDENCE;
        boolean rightLegValid = rightHip[2] > MIN_CONFIDENCE && rightKnee[2] > MIN_CONFIDENCE;
        
        // DETAILED COORDINATE LOGGING for debugging
        Log.d(TAG, String.format("🔍 COORDINATES - LeftHip: [%.3f,%.3f,%.3f] RightHip: [%.3f,%.3f,%.3f]", 
                leftHip[0], leftHip[1], leftHip[2], rightHip[0], rightHip[1], rightHip[2]));
        Log.d(TAG, String.format("🔍 COORDINATES - LeftKnee: [%.3f,%.3f,%.3f] RightKnee: [%.3f,%.3f,%.3f]", 
                leftKnee[0], leftKnee[1], leftKnee[2], rightKnee[0], rightKnee[1], rightKnee[2]));
        Log.d(TAG, String.format("🔍 CONFIDENCE - LeftLeg: %s (%.3f,%.3f) RightLeg: %s (%.3f,%.3f)", 
                leftLegValid, leftHip[2], leftKnee[2], rightLegValid, rightHip[2], rightKnee[2]));
        
        if (!leftLegValid && !rightLegValid) {
            Log.d(TAG, "❌ Not enough confidence in hip/knee keypoints");
            return;
        }
        
        // Skip first frame - need to establish baseline
        if (!hasValidPrevFrame) {
            hasValidPrevFrame = true;
            Log.d(TAG, "✅ First frame recorded for high knee detection");
            return;
        }
        
        // Track left leg high knee using simple upward movement
        if (leftLegValid) {
            boolean leftKneeLifted = leftKneeTracker.processKneeUpwardMovement(leftKnee[1]);
            if (leftKneeLifted) {
                highKneeCount++;
                Log.d(TAG, "🦵 LEFT HIGH KNEE detected! Count: " + highKneeCount);
                notifyListener();
            }
        }
        
        // Track right leg high knee using simple upward movement  
        if (rightLegValid) {
            boolean rightKneeLifted = rightKneeTracker.processKneeUpwardMovement(rightKnee[1]);
            if (rightKneeLifted) {
                highKneeCount++;
                Log.d(TAG, "🦵 RIGHT HIGH KNEE detected! Count: " + highKneeCount);
                notifyListener();
            }
        }
    }
    
    /**
     * Notify listener of new high knee count
     */
    private void notifyListener() {
        if (listener != null) {
            final int count = highKneeCount;
            mainHandler.post(() -> listener.onHighKneeDetected(count));
        }
    }
    
    /**
     * Inner class to track individual knee lifting for each leg using simple upward movement
     */
    private static class KneeTracker {
        private final String legName;
        private boolean isKneeUp = false; // Track if knee is currently lifted
        private long lastCountTime = 0; // Track last count time for cooldown
        private float baselineY = -1f; // Baseline knee Y position (established over first frames)
        private int frameCount = 0; // Count frames to establish baseline
        private float sumY = 0f; // Sum of Y positions for baseline calculation
        
        public KneeTracker(String legName) {
            this.legName = legName;
        }
        
        /**
         * Process knee position using simple upward movement detection
         * @param kneeY Y position of knee
         * @return true if a new high knee lift is detected
         */
        public boolean processKneeUpwardMovement(float kneeY) {
            long currentTime = System.currentTimeMillis();
            
            // Establish baseline over first BASELINE_FRAMES frames
            if (frameCount < BASELINE_FRAMES) {
                sumY += kneeY;
                frameCount++;
                if (frameCount == BASELINE_FRAMES) {
                    baselineY = sumY / BASELINE_FRAMES;
                    Log.d(TAG, "✅ " + legName + " baseline established: " + baselineY);
                }
                return false; // Don't count during baseline establishment
            }
            
            // Calculate how much knee moved up from baseline (negative = up since Y is inverted)
            float upwardMovement = baselineY - kneeY; // Positive = moved up from baseline
            
            // Determine if knee should be considered "up" or "down"
            boolean shouldGoUp = upwardMovement > UPWARD_MOVEMENT_THRESHOLD;
            boolean shouldGoDown = upwardMovement < DOWN_MOVEMENT_THRESHOLD;
            
            Log.d(TAG, String.format("%s knee - BaselineY: %.3f, CurrentY: %.3f, UpMovement: %.3f, ShouldUp: %s, ShouldDown: %s, IsUp: %s, Cooldown: %s", 
                    legName, baselineY, kneeY, upwardMovement, shouldGoUp, shouldGoDown, isKneeUp, 
                    (currentTime - lastCountTime) < COOLDOWN_MS ? "YES" : "NO"));
            
            // STATE TRANSITIONS with cooldown protection
            if (shouldGoUp && !isKneeUp) {
                // Check cooldown - prevent counting too frequently
                if (currentTime - lastCountTime < COOLDOWN_MS) {
                    Log.d(TAG, "⏰ " + legName + " knee lift blocked by cooldown");
                    return false;
                }
                
                // Knee moved up significantly from baseline - count it!
                isKneeUp = true;
                lastCountTime = currentTime;
                Log.d(TAG, "✅ " + legName + " knee LIFTED (moved up from baseline) - new high knee detected!");
                return true; // New high knee detected
            }
            
            // Detect knee moving back down
            if (shouldGoDown && isKneeUp) {
                // Knee moved back down to baseline - ready for next lift
                isKneeUp = false;
                Log.d(TAG, "⬇️ " + legName + " knee LOWERED (back to baseline) - ready for next lift");
            }
            
            return false; // No new high knee detected
        }
        
        public void reset() {
            isKneeUp = false;
            lastCountTime = 0;
            baselineY = -1f;
            frameCount = 0;
            sumY = 0f;
            Log.d(TAG, legName + " knee tracker reset");
        }
    }
    
    /**
     * Reset the high knee counter to zero
     */
    public void reset() {
        highKneeCount = 0;
        hasValidPrevFrame = false;
        leftKneeTracker.reset();
        rightKneeTracker.reset();
        Log.d(TAG, "High knee counter reset");
    }
    
    /**
     * Get current high knee count
     */
    public int getHighKneeCount() {
        return highKneeCount;
    }
} 