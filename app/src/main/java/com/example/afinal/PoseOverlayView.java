package com.example.afinal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PoseOverlayView extends View {
    // Store keypoints efficiently with confidence smoothing
    private List<float[]> keypoints = new ArrayList<>();
    private List<float[]> smoothedKeypoints = new ArrayList<>(); // For stability
    
    // Pre-allocate paints for better performance
    private final Paint pointPaint;
    private final Paint linePaint;
    private final Paint confidencePaint; // New: for confidence visualization
    private final Paint backgroundPaint; // New: for keypoint backgrounds
    
    // Connections between body parts (pairs of keypoint indices)
    private final int[][] connections = {
        {5, 7}, {7, 9}, {6, 8}, {8, 10}, // arms
        {5, 6}, {5, 11}, {6, 12}, // shoulders to hips
        {11, 12}, // hips
        {11, 13}, {13, 15}, {12, 14}, {14, 16} // legs
    };
    
    // Enhanced colors for better visibility
    private final int[] keypointColors = {
        Color.RED,     // 0 - Nose
        Color.MAGENTA, // 1-4 - Eyes and ears
        Color.MAGENTA,
        Color.MAGENTA,
        Color.MAGENTA,
        Color.YELLOW,  // 5-10 - Shoulders, elbows, wrists
        Color.YELLOW,
        Color.CYAN,
        Color.CYAN,
        Color.GREEN,
        Color.GREEN,
        Color.BLUE,    // 11-16 - Hips, knees, ankles
        Color.BLUE,
        Color.WHITE,
        Color.WHITE,
        Color.GRAY,
        Color.GRAY
    };
    
    // Constants for optimized drawing
    private static final float POINT_SIZE = 12f; // Slightly larger for better visibility
    private static final float LINE_WIDTH = 5f;   // Thicker lines
    private static final float MIN_CONFIDENCE = 0.01f; // Very low threshold for immediate visibility
    private static final float SMOOTHING_FACTOR = 0.1f; // Minimal smoothing for instant response
    
    // New: Performance tracking
    private long lastUpdateTime = 0;
    private static final long MIN_UPDATE_INTERVAL = 8; // ~120 FPS for maximum responsiveness

    public PoseOverlayView(Context context) {
        this(context, null);
    }

    public PoseOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PoseOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        
        // Initialize paints once to avoid garbage collection
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setStyle(Paint.Style.FILL);
        
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(LINE_WIDTH);
        linePaint.setStrokeCap(Paint.Cap.ROUND); // Rounded endpoints
        
        // New paints for enhanced visualization
        confidencePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        confidencePaint.setStyle(Paint.Style.STROKE);
        confidencePaint.setStrokeWidth(2f);
        
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setAlpha(100); // Semi-transparent background
        
        // Use software rendering for consistent performance across devices
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        
        // Initialize empty keypoints
        initializeEmptyKeypoints();
    }
    
    private void initializeEmptyKeypoints() {
        keypoints.clear();
        smoothedKeypoints.clear();
        for (int i = 0; i < 17; i++) {
            keypoints.add(new float[]{0, 0, 0});
            smoothedKeypoints.add(new float[]{0, 0, 0});
        }
    }

    public void setKeypoints(List<float[]> newKeypoints) {
        // Remove rate limiting for immediate landmark display
        if (newKeypoints == null || newKeypoints.isEmpty()) {
            return;
        }
        
        // Use mostly raw data for instant response with minimal smoothing only for very jittery points
        if (smoothedKeypoints.isEmpty() || smoothedKeypoints.size() != newKeypoints.size()) {
            this.keypoints = new ArrayList<>(newKeypoints);
            this.smoothedKeypoints = new ArrayList<>(newKeypoints);
        } else {
            // Apply extremely minimal smoothing only to prevent jitter
            for (int i = 0; i < Math.min(newKeypoints.size(), smoothedKeypoints.size()); i++) {
                float[] newKp = newKeypoints.get(i);
                float[] smoothedKp = smoothedKeypoints.get(i);
                
                // Use raw data for immediate response, only smooth if movement is very small (jitter)
                float deltaX = Math.abs(newKp[0] - smoothedKp[0]);
                float deltaY = Math.abs(newKp[1] - smoothedKp[1]);
                
                if (newKp[2] > 0.3f && (deltaX < 0.01f || deltaY < 0.01f)) {
                    // Very small movement - apply minimal smoothing to reduce jitter
                    smoothedKp[0] = SMOOTHING_FACTOR * smoothedKp[0] + (1 - SMOOTHING_FACTOR) * newKp[0];
                    smoothedKp[1] = SMOOTHING_FACTOR * smoothedKp[1] + (1 - SMOOTHING_FACTOR) * newKp[1];
                } else {
                    // Normal or large movement - use raw data for instant response
                    smoothedKp[0] = newKp[0];
                    smoothedKp[1] = newKp[1];
                }
                smoothedKp[2] = newKp[2]; // Always use raw confidence
            }
            this.keypoints = smoothedKeypoints;
        }
        
        // Use animation-friendly invalidation
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (keypoints == null || keypoints.isEmpty()) {
            return;
        }

        final float width = getWidth();
        final float height = getHeight();
        
        if (width <= 0 || height <= 0) {
            return;
        }
        
        // Draw in layers: confidence circles, connections, then points on top
        drawConfidenceIndicators(canvas, width, height);
        drawConnections(canvas, width, height);
        drawKeypoints(canvas, width, height);
    }
    
    private void drawConfidenceIndicators(Canvas canvas, float width, float height) {
        // Draw confidence circles around high-confidence keypoints
        for (int i = 0; i < keypoints.size(); i++) {
            float[] point = keypoints.get(i);
            float confidence = point[2];
            
            if (confidence > 0.5f) { // Only for high confidence points
                float x = point[0] * width;
                float y = point[1] * height;
                
                confidencePaint.setColor(Color.GREEN);
                confidencePaint.setAlpha((int)(confidence * 100));
                
                // Draw confidence circle
                canvas.drawCircle(x, y, POINT_SIZE + 8, confidencePaint);
            }
        }
    }
    
    private void drawConnections(Canvas canvas, float width, float height) {
        for (int[] connection : connections) {
            int start = connection[0];
            int end = connection[1];
            
            if (start >= keypoints.size() || end >= keypoints.size()) {
                continue;
            }
            
            float[] startPoint = keypoints.get(start);
            float[] endPoint = keypoints.get(end);
            
            // Skip low confidence connections
            if (startPoint[2] < MIN_CONFIDENCE || endPoint[2] < MIN_CONFIDENCE) {
                continue;
            }
            
            float startX = startPoint[0] * width;
            float startY = startPoint[1] * height;
            float endX = endPoint[0] * width;
            float endY = endPoint[1] * height;
            
            // Set alpha based on confidence with minimum visibility
            float avgConfidence = (startPoint[2] + endPoint[2]) / 2;
            int alpha = Math.max(100, (int)(avgConfidence * 255));
            
            // Color connections based on body part
            if ((start >= 5 && start <= 10) || (end >= 5 && end <= 10)) {
                linePaint.setColor(Color.YELLOW); // Arms
            } else if ((start >= 11 && start <= 16) || (end >= 11 && end <= 16)) {
                linePaint.setColor(Color.BLUE); // Legs
            } else {
                linePaint.setColor(Color.CYAN); // Torso
            }
            
            linePaint.setAlpha(alpha);
            canvas.drawLine(startX, startY, endX, endY, linePaint);
        }
    }
    
    private void drawKeypoints(Canvas canvas, float width, float height) {
        for (int i = 0; i < keypoints.size(); i++) {
            float[] point = keypoints.get(i);
            float confidence = point[2];
            
            // Skip very low confidence keypoints
            if (confidence < MIN_CONFIDENCE) {
                continue;
            }
            
            float x = point[0] * width;
            float y = point[1] * height;
            
            // Draw background circle for better visibility
            backgroundPaint.setAlpha((int)(confidence * 150));
            canvas.drawCircle(x, y, POINT_SIZE + 4, backgroundPaint);
            
            // Set color and alpha based on confidence
            pointPaint.setColor(keypointColors[i]);
            pointPaint.setAlpha(Math.max(150, (int)(confidence * 255)));
            
            // Draw keypoint with size based on confidence
            float pointSize = POINT_SIZE * (0.5f + confidence * 0.5f);
            canvas.drawCircle(x, y, pointSize, pointPaint);
            
            // Draw small number for keypoint identification (optional, for debugging)
            if (confidence > 0.8f) {
                Paint textPaint = new Paint();
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(20);
                textPaint.setAntiAlias(true);
                canvas.drawText(String.valueOf(i), x + 15, y - 10, textPaint);
            }
        }
    }
    
    // New method to get pose detection quality
    public float getPoseQuality() {
        if (keypoints.isEmpty()) return 0f;
        
        float totalConfidence = 0f;
        int validPoints = 0;
        
        for (float[] point : keypoints) {
            if (point[2] > MIN_CONFIDENCE) {
                totalConfidence += point[2];
                validPoints++;
            }
        }
        
        return validPoints > 0 ? (totalConfidence / validPoints) * (validPoints / 17f) : 0f;
    }
    
    // New method to check if pose is centered
    public boolean isPoseCentered() {
        if (keypoints.isEmpty()) return false;
        
        // Check if main body parts are in frame
        float[] nose = keypoints.get(0);
        float[] leftShoulder = keypoints.get(5);
        float[] rightShoulder = keypoints.get(6);
        
        if (nose[2] > MIN_CONFIDENCE && leftShoulder[2] > MIN_CONFIDENCE && rightShoulder[2] > MIN_CONFIDENCE) {
            float centerX = (leftShoulder[0] + rightShoulder[0]) / 2;
            return centerX > 0.3f && centerX < 0.7f; // Reasonably centered
        }
        
        return false;
    }
}