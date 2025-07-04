package com.example.afinal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.List;

/**
 * ArmCircleCounter - Detects arm circle movements by tracking circular motion of arms around shoulders
 * Arms should be extended parallel to the floor, rotating around the shoulder joint
 */
public class ArmCircleCounter {
    public interface ArmCircleListener {
        void onArmCircleDetected(int armCircleCount);
    }
    
    // Arm circle detection constants
    //private static final float MOVEMENT_THRESHOLD = 0.008f; // Smaller threshold for small circles
    private static final float MIN_CONFIDENCE = 0.25f; // Slightly lowered
    private static final long CIRCLE_COOLDOWN_MS = 1000; // Cooldown time
    //private static final float MIN_CIRCLE_RADIUS = 0.03f; // Maybe smaller? minimum radius for small circles
    //private static final int CIRCLE_VALIDATION_POINTS = 4; // Very few points needed for tiny circles
    private static final float ARM_EXTENSION_THRESHOLD = 0.05f; // Even lower threshold for arm extension???
    
    // Human-realistic validation constants - adjusted for natural movement
    private static final float HORIZONTAL_ARM_TOLERANCE = 0.35f; // Much more toleranced
    //private static final float MAX_VERTICAL_DEVIATION = 0.5f; // Allow more Y movement
    //private static final int MIN_FRAMES_FOR_CIRCLE = 4; // Even fewer frames needed
    //private static final float BOTH_ARMS_REQUIREMENT = 0.3f;
    
    private static final String TAG = "ArmCircleCounter";
    
    // Circle tracking state
    private int armCircleCount = 0;
    private boolean hasValidPrevFrame = false;
    private long lastCircleTime = 0;
    
    // 10-POINT FLEXIBILITY: ///>>>>>>Track recent completions for "near simultaneous" detection
    private long leftArmLastCompletion = 0;
    private long rightArmLastCompletion = 0;
    private static final long SIMULTANEOUS_WINDOW_MS = 400; // 400ms window for "simultaneous" completion
    
    // Circle detection for left and right arms >>>>>>>>tracking arm position relative to shoulder
    private ArmCircleTracker leftArmTracker = new ArmCircleTracker();
    private ArmCircleTracker rightArmTracker = new ArmCircleTracker();
    
    // Callback
    private ArmCircleListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public ArmCircleCounter(ArmCircleListener listener) {
        this.listener = listener;
    }
    
    /**
     * Validate if arms are in proper position for arm circles using shoulder,elbow,wrist
     * Arms should be extended horizontally with all three joints aligned
     *>>>>>>>>>>>>>>>>>> DONT FORGET THESE!!!<<<
     */
    private boolean isValidArmCirclePosition(float[] leftShoulder, float[] rightShoulder, 
                                           float[] leftElbow, float[] rightElbow,
                                           float[] leftWrist, float[] rightWrist) {
        
        // >>Check<<< if we have confidence in ALL required keypoints (shoulder, elbow, wrist)
        boolean allPointsValid = leftShoulder[2] > MIN_CONFIDENCE && rightShoulder[2] > MIN_CONFIDENCE &&
                               leftElbow[2] > MIN_CONFIDENCE && rightElbow[2] > MIN_CONFIDENCE &&
                               leftWrist[2] > MIN_CONFIDENCE && rightWrist[2] > MIN_CONFIDENCE;
        
        if (!allPointsValid) {
            Log.d(TAG, "❌❌❌❌❌❌ Not all arm keypoints visible (shoulder-elbow-wrist)");
            return false;
        }
        
        // Check if arms are properly extended using all three points
        // Calculate arm straightness: shoulder->elbow->wrist should be roughly aligned
        float leftArmStraightness = calculateArmStraightness(leftShoulder, leftElbow, leftWrist);
        float rightArmStraightness = calculateArmStraightness(rightShoulder, rightElbow, rightWrist);
        
        if (leftArmStraightness < 0.7f || rightArmStraightness < 0.7f) {
            Log.d(TAG, "❌❌❌❌❌❌ Arms not extended straight - Left: " + leftArmStraightness + ", Right: " + rightArmStraightness);
            return false;
        }
        
        // Check total arm length (shoulder to wrist)
        float leftArmLength = calculateDistance(leftShoulder, leftWrist);
        float rightArmLength = calculateDistance(rightShoulder, rightWrist);
        
        if (leftArmLength < ARM_EXTENSION_THRESHOLD * 1.5f || rightArmLength < ARM_EXTENSION_THRESHOLD * 1.5f) {
            Log.d(TAG, "❌❌❌❌❌❌ Arms not extended enough - Left: " + leftArmLength + ", Right: " + rightArmLength);
            return false;
        }
        
        // Check if arms are roughly horizontal (wrists near shoulder height)
        float leftVerticalDiff = Math.abs(leftWrist[1] - leftShoulder[1]);
        float rightVerticalDiff = Math.abs(rightWrist[1] - rightShoulder[1]);
        
        if (leftVerticalDiff > HORIZONTAL_ARM_TOLERANCE || rightVerticalDiff > HORIZONTAL_ARM_TOLERANCE) {
            Log.d(TAG, "❌❌❌❌❌❌❌ Arms not horizontal - Left diff: " + leftVerticalDiff + ", Right diff: " + rightVerticalDiff);
            return false;
        }
        
        // Check if both arms are at roughly same height (prevent one arm raised)
        float armHeightDiff = Math.abs(leftWrist[1] - rightWrist[1]);
        if (armHeightDiff > HORIZONTAL_ARM_TOLERANCE) {
            Log.d(TAG, "❌❌❌❌❌❌❌ Arms not at same height - Difference: " + armHeightDiff);
            return false;
        }
        
        Log.d(TAG, "✅✅✅✅✅✅✅ Perfect arm circle position - both arms extended horizontally");
        return true;
    }
    
    /**
     * Calculate how straight the arm is (shoulder->elbow->wrist alignment)
     * Returns value 0.0 to 1.0 where 1.0 is perfectly straight
     * MAYBE LOWERRR THIS????
     */
    private float calculateArmStraightness(float[] shoulder, float[] elbow, float[] wrist) {
        float shoulderElbowDist = calculateDistance(shoulder, elbow);
        float elbowWristDist = calculateDistance(elbow, wrist);
        float shoulderWristDist = calculateDistance(shoulder, wrist);
        
        // If arm is perfectly straight, shoulder-wrist distance equals sum of segments
        float expectedDist = shoulderElbowDist + elbowWristDist;
        return shoulderWristDist / expectedDist; // Closer to 1.0 = straighter arm
    }
    
    /**
     * Calculate 2D distance between two points
     */
    private float calculateDistance(float[] point1, float[] point2) {
        return (float) Math.sqrt(
            Math.pow(point1[0] - point2[0], 2) + 
            Math.pow(point1[1] - point2[1], 2)
        );
    }
    
    /**
     * Process keypoints to detect arm circle movements
     * Tracks the rotation of arms around shoulder joints
     * >>>>>>>WORKING DO NOT TOUCH AGAIN<<<
     */
    public void processKeypoints(java.util.List<float[]> keypoints) {
        if (keypoints == null || keypoints.size() < 17) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Get shoulder, elbow, and wrist keypoints for proper arm circle detection in 2D
        float[] leftShoulder = keypoints.get(5);   // Left shoulder (pivot point)
        float[] rightShoulder = keypoints.get(6);  // Right shoulder (pivot point)
        float[] leftElbow = keypoints.get(7);      // Left elbow (mid arm)
        float[] rightElbow = keypoints.get(8);     // Right elbow (mid arm)
        float[] leftWrist = keypoints.get(9);      // Left wrist (end of arm)
        float[] rightWrist = keypoints.get(10);    // Right wrist (end of arm)
        
        //  STRICT VALIDATION: Both arms must be in arm circle position using all joints
        if (!isValidArmCirclePosition(leftShoulder, rightShoulder, leftElbow, rightElbow, leftWrist, rightWrist)) {
            // >>>Reset trackers if position is invalid to prevent false positives
            leftArmTracker.reset();
            rightArmTracker.reset();
            Log.d(TAG, "❌❌❌❌ Arms not in proper arm circle position - resetting");
            return;
        }
        
        // All arm joints must be valid (already checked in validation, but double-check)
        boolean leftArmValid = leftShoulder[2] > MIN_CONFIDENCE && leftElbow[2] > MIN_CONFIDENCE && leftWrist[2] > MIN_CONFIDENCE;
        boolean rightArmValid = rightShoulder[2] > MIN_CONFIDENCE && rightElbow[2] > MIN_CONFIDENCE && rightWrist[2] > MIN_CONFIDENCE;
        
        if (!leftArmValid || !rightArmValid) {
            Log.d(TAG, "❌❌❌❌ All arm joints must be visible (shoulder-elbow-wrist)");
            leftArmTracker.reset();
            rightArmTracker.reset();
            return;
        }
        
        // Skip first frame - need to establish baseline
        // SOLVED!!!
        if (!hasValidPrevFrame) {
            hasValidPrevFrame = true;
            if (leftArmValid) {
                leftArmTracker.initialize(leftShoulder[0], leftShoulder[1], leftElbow[0], leftElbow[1], leftWrist[0], leftWrist[1]);
            }
            if (rightArmValid) {
                rightArmTracker.initialize(rightShoulder[0], rightShoulder[1], rightElbow[0], rightElbow[1], rightWrist[0], rightWrist[1]);
            }
            Log.d(TAG, "✅ First frame recorded with all arm joints (shoulder-elbow-wrist)");
            return;
        }
        
        // Track both arms using shoulder-elbow-wrist for better 2D circular motion detection
        leftArmTracker.addArmPosition(leftShoulder[0], leftShoulder[1], leftElbow[0], leftElbow[1], leftWrist[0], leftWrist[1]);
        rightArmTracker.addArmPosition(rightShoulder[0], rightShoulder[1], rightElbow[0], rightElbow[1], rightWrist[0], rightWrist[1]);
        
        // 10-POINT FLEXIBILITY: Check for circle completion with timing window
        boolean leftCircleCompleted = leftArmTracker.isCircleCompleted();
        boolean rightCircleCompleted = rightArmTracker.isCircleCompleted();
        
        // Track completion times for flexible "simultaneous" detection
        if (leftCircleCompleted) {
            leftArmLastCompletion = currentTime;
            Log.d(TAG, "✅ LEFT ARM COMPLETED CIRCLE at " + currentTime);
        }
        if (rightCircleCompleted) {
            rightArmLastCompletion = currentTime;
            Log.d(TAG, "✅ RIGHT ARM COMPLETED CIRCLE at " + currentTime);
        }
        
        // FLEXIBLE SIMULTANEOUS DETECTION: <>>>>>>>>>Count if both arms completed within time window
        long timeDiff = Math.abs(leftArmLastCompletion - rightArmLastCompletion);
        boolean bothRecentlyCompleted = (leftArmLastCompletion > 0 && rightArmLastCompletion > 0) && 
                                       (timeDiff <= SIMULTANEOUS_WINDOW_MS);
        boolean stillInWindow = (currentTime - Math.max(leftArmLastCompletion, rightArmLastCompletion)) < SIMULTANEOUS_WINDOW_MS;
        
        boolean circleDetected = bothRecentlyCompleted && stillInWindow;
        
        if (circleDetected) {
            Log.d(TAG, "🎯 BOTH ARMS COMPLETED WITHIN " + timeDiff + "ms - COUNTING AS VALID REP!");
            // Reset >>>>> prevent double counting
            leftArmLastCompletion = 0;
            rightArmLastCompletion = 0;
        } else if (leftCircleCompleted || rightCircleCompleted) {
            Log.d(TAG, "⏳ One arm completed - waiting for other arm within " + SIMULTANEOUS_WINDOW_MS + "ms window");
            // Don't reset immediately!!>>>>>>> give other arm a chance to complete
        }
        
        // TIMEOUT CLEANUP: Clear old completion times if they're too PASSED
        if (leftArmLastCompletion > 0 && (currentTime - leftArmLastCompletion) > SIMULTANEOUS_WINDOW_MS) {
            Log.d(TAG, "⏰ Left arm completion timed out - clearing");
            leftArmLastCompletion = 0;
            leftArmTracker.reset(); // Reset to prevent false positives
        }
        if (rightArmLastCompletion > 0 && (currentTime - rightArmLastCompletion) > SIMULTANEOUS_WINDOW_MS) {
            Log.d(TAG, "⏰ Right arm completion timed out - clearing");
            rightArmLastCompletion = 0;
            rightArmTracker.reset(); // Reset to prevent false positives
        }
        
        // Check if we detected a circle and cooldown has passed
        if (circleDetected && currentTime - lastCircleTime > CIRCLE_COOLDOWN_MS) {
            armCircleCount++;
            lastCircleTime = currentTime;
            
            Log.d(TAG, "🎯 ARM CIRCLE COMPLETED! Count: " + armCircleCount + " (Cooldown enforced)");
            
            // IMMEDIATELY reset trackers to prevent double counting
            leftArmTracker.reset();
            rightArmTracker.reset();
            
            // Notify listener
            if (listener != null) {
                final int count = armCircleCount;
                mainHandler.post(() -> listener.onArmCircleDetected(count));
            }
        } else if (circleDetected) {
            // Circle detected but still in cooldown - reset to prevent double counting
            Log.d(TAG, "🕐 Circle detected but in cooldown - resetting trackers");
            leftArmTracker.reset();
            rightArmTracker.reset();
        }
    }
    
    /**
     * SIMPLE approach: Track arm circles using peak detection (like heartbeat monitor)
     * Detect high and low points of wrist Y-position relative to shoulder
     */
    private static class ArmCircleTracker {
        private float shoulderX, shoulderY; // Shoulder position (pivot point)
        private boolean isInitialized = false;
        private long lastCompletionTime = 0; // Track last completion to prevent rapid fire
        
        // PEAK DETECTION: Track wrist Y position relative to shoulder
        private float prevWristY = 0f;
        private float currentWristY = 0f;
        private boolean wasGoingUp = false;
        private boolean wasGoingDown = false;
        private int peakCount = 0; // Count peaks and valleys
        private float peakThreshold = 0.02f; // Minimum movement to count as peak/valley
        
        // Track the cycle: need to see high→low→high or low→high→low for complete circle
        private boolean hasSeenHigh = false;
        private boolean hasSeenLow = false;
        
                public void initialize(float shoulderX, float shoulderY, float elbowX, float elbowY, float wristX, float wristY) {
            this.shoulderX = shoulderX;
            this.shoulderY = shoulderY;
            
            // SIMPLE: Just store initial wrist Y position relative to shoulder
            currentWristY = wristY - shoulderY; // Relative Y position
            prevWristY = currentWristY;
            
            isInitialized = true;
            peakCount = 0;
            hasSeenHigh = false;
            hasSeenLow = false;
            wasGoingUp = false;
            wasGoingDown = false;
            
            Log.d(TAG, String.format("✅ Simple arm tracker initialized - Initial Y: %.3f", currentWristY));
        }
        
        public void addArmPosition(float shoulderX, float shoulderY, float elbowX, float elbowY, float wristX, float wristY) {
            if (!isInitialized) {
                initialize(shoulderX, shoulderY, elbowX, elbowY, wristX, wristY);
                return;
            }
            
            // Update shoulder position
            this.shoulderX = shoulderX;
            this.shoulderY = shoulderY;
            
            // SIMPLE PEAK DETECTION: Track wrist Y position relative to shoulder
            prevWristY = currentWristY;
            currentWristY = wristY - shoulderY; // Relative Y position
            
            float yDiff = currentWristY - prevWristY;
            
            // Only process significant movements
            if (Math.abs(yDiff) < peakThreshold) {
                return; // Too small movement to be meaningful
            }
            
            // Detect direction changes (peaks and valleys)
            boolean isGoingUp = yDiff < 0; // Negative Y = going up (screen coordinates)
            boolean isGoingDown = yDiff > 0; // Positive Y = going down
            
            // Detect peak (was going up, now going down)
            if (wasGoingUp && isGoingDown) {
                hasSeenHigh = true;
                peakCount++;
                Log.d(TAG, "📈 Peak detected (high point) - Count: " + peakCount);
            }
            
            // Detect valley (was going down, now going up)
            if (wasGoingDown && isGoingUp) {
                hasSeenLow = true;
                peakCount++;
                Log.d(TAG, "📉 Valley detected (low point) - Count: " + peakCount);
            }
            
            // Update direction tracking
            wasGoingUp = isGoingUp;
            wasGoingDown = isGoingDown;
            
            Log.d(TAG, String.format("Y-Track: %.3f → %.3f (diff: %.3f) | Peaks: %d | High: %s, Low: %s", 
                    prevWristY, currentWristY, yDiff, peakCount, hasSeenHigh, hasSeenLow));
        }
        

        
                         public boolean isCircleCompleted() {
            if (!isInitialized) {
                return false;
            }
            
            // Prevent rapid successive completions
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCompletionTime < 500) { // 500ms cooldown between circles
                return false;
            }
            
            // SIMPLE CIRCLE DETECTION: Need to see both high and low points + enough peaks
            // A full circle should have at least 2-3 direction changes (peaks/valleys)
            boolean hasFullCycle = hasSeenHigh && hasSeenLow;
            boolean hasEnoughPeaks = peakCount >= 2; // At least 2 direction changes
            
            boolean isCompleted = hasFullCycle && hasEnoughPeaks;
            
            Log.d(TAG, String.format("CIRCLE CHECK - Peaks: %d, High: %s, Low: %s, COMPLETED: %s", 
                    peakCount, hasSeenHigh, hasSeenLow, isCompleted));
            
            if (isCompleted) {
                lastCompletionTime = currentTime;
                Log.d(TAG, "✅ SIMPLE CIRCLE COMPLETED - Resetting for next");
                resetForNextCircle();
            }
            
            return isCompleted;
        }
        
        /**
         * Reset tracker state for the next circle
         */
        private void resetForNextCircle() {
            peakCount = 0;
            hasSeenHigh = false;
            hasSeenLow = false;
            wasGoingUp = false;
            wasGoingDown = false;
            Log.d(TAG, "🔄 Simple tracker reset - ready for next circle");
        }
        
        public void reset() {
            isInitialized = false;
            lastCompletionTime = 0;
            prevWristY = 0f;
            currentWristY = 0f;
            peakCount = 0;
            hasSeenHigh = false;
            hasSeenLow = false;
            wasGoingUp = false;
            wasGoingDown = false;
            Log.d(TAG, "Simple arm tracker fully reset - clean state");
        }
    }
    
    public void reset() {
        armCircleCount = 0;
        hasValidPrevFrame = false;
        leftArmTracker.reset();
        rightArmTracker.reset();
        lastCircleTime = 0;
        // Reset timing window tracking
        leftArmLastCompletion = 0;
        rightArmLastCompletion = 0;
        Log.d(TAG, "Arm circle counter reset with timing flexibility");
    }
    
    public int getArmCircleCount() {
        return armCircleCount;
    }
    
    public long getLastCircleTime() {
        return lastCircleTime;
    }
} 