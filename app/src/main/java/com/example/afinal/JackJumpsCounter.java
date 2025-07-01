package com.example.afinal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * JackJumpsCounter - Detects jumping jacks by tracking jump motion + arm/leg spread
 */
public class JackJumpsCounter {
    public interface JackJumpsListener {
        void onJackJumpDetected(int jumpCount);
    }
    
    // Jumping jack detection constants
    private static final float MIN_CONFIDENCE = 0.3f;
    private static final float JUMP_MOVEMENT_THRESHOLD = 0.015f; // Vertical movement threshold
    private static final float ARM_UP_THRESHOLD = 0.05f; // Arms above shoulder level
    // private static final float LEG_SPREAD_THRESHOLD = 0.05f; // Legs spread wider than hips >>> closed for now
    private static final long JACK_JUMP_COOLDOWN_MS = 800; // Shorter cooldown for faster detection 
    private static final String TAG = "JackJumpsCounter";
    
    // State tracking
    private int jackJumpCount = 0;
    private long lastJackJumpTime = 0;
    private boolean isInJackJumpPosition = false;
    private boolean hasValidPrevFrame = false;
    
    // Previous frame data for movement detection
    private float prevBodyCenterY = 0f;
    
    // Callback
    private JackJumpsListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public JackJumpsCounter(JackJumpsListener listener) {
        this.listener = listener;
    }
    
    /**
     * Simplified jumping jacks detection:
     * 1. Vertical jump movement (body going up)
     * 2. Arms raised above shoulders  
     * (No leg detection - just arms and body movement)
     */
    public void processKeypoints(java.util.List<float[]> keypoints) {
        if (keypoints == null || keypoints.size() < 17) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Get key body parts for jumping jack detection
        float[] leftShoulder = keypoints.get(5);
        float[] rightShoulder = keypoints.get(6);
        float[] leftWrist = keypoints.get(9);
        float[] rightWrist = keypoints.get(10);
        float[] leftHip = keypoints.get(11);
        float[] rightHip = keypoints.get(12);
        
        // Check basic confidence
        if (leftShoulder[2] < MIN_CONFIDENCE || rightShoulder[2] < MIN_CONFIDENCE) {
            return;
        }
        
        // Calculate body center for jump detection
        float bodyCenterY = (leftShoulder[1] + rightShoulder[1] + leftHip[1] + rightHip[1]) / 4.0f;
        
        // Skip first frame (need previous frame for comparison)
        if (!hasValidPrevFrame) {
            prevBodyCenterY = bodyCenterY;
            hasValidPrevFrame = true;
            return;
        }
        
        // 1. Check for vertical jump movement (negative = upward)
        float verticalMovement = bodyCenterY - prevBodyCenterY;
        boolean isJumping = verticalMovement < -JUMP_MOVEMENT_THRESHOLD;
        
        // 2. Check arms are raised (wrists above shoulders)
        boolean armsRaised = false;
        if (leftWrist[2] > MIN_CONFIDENCE && rightWrist[2] > MIN_CONFIDENCE) {
            float leftArmRaise = leftShoulder[1] - leftWrist[1]; // Positive = wrist above shoulder
            float rightArmRaise = rightShoulder[1] - rightWrist[1]; // Positive = wrist above shoulder
            armsRaised = (leftArmRaise > ARM_UP_THRESHOLD) && (rightArmRaise > ARM_UP_THRESHOLD);
        }
        
        // Cooldown check
        boolean cooldownPassed = currentTime - lastJackJumpTime > JACK_JUMP_COOLDOWN_MS;
        
        // Simplified jack jump detection: Just arms up + jumping (no legs)
        boolean isValidJackJump = isJumping && armsRaised;
        
        // Debug logging
        Log.d(TAG, String.format("Jump:%.3f Arms:%s Valid:%s Cool:%s", 
              verticalMovement, armsRaised, isValidJackJump, cooldownPassed));
        
        // Detect jack jump
        if (isValidJackJump && !isInJackJumpPosition && cooldownPassed) {
            isInJackJumpPosition = true;
            jackJumpCount++;
            lastJackJumpTime = currentTime;
            
            Log.d(TAG, "✅ JACK JUMP DETECTED! Count: " + jackJumpCount);
            
            if (listener != null) {
                final int count = jackJumpCount;
                mainHandler.post(() -> listener.onJackJumpDetected(count));
            }
        }
        
        // Reset position when not in jack jump pose
        if (!isValidJackJump) {
            isInJackJumpPosition = false;
        }
        
        // Update previous frame data
        prevBodyCenterY = bodyCenterY;
    }
    
    /**
     * Reset the jack jumps counter
     */
    public void reset() {
        jackJumpCount = 0;
        isInJackJumpPosition = false;
        lastJackJumpTime = 0;
        hasValidPrevFrame = false;
        prevBodyCenterY = 0f;
        Log.d(TAG, "Jack jumps counter reset");
    }
    
    /**
     * Get current jack jump count
     */
    public int getJackJumpCount() {
        return jackJumpCount;
    }
    
    /**
     * Get current state for debugging
     */
    public String getCurrentState() {
        return isInJackJumpPosition ? "JACK_JUMP" : "READY";
    }
} 