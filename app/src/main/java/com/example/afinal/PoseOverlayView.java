package com.example.afinal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PoseOverlayView extends View {
    // Store keypoints efficiently
    private List<float[]> keypoints = new ArrayList<>();
    
    // Pre-allocate paints for better performance
    private final Paint pointPaint;
    private final Paint linePaint;
    
    // Connections between body parts (pairs of keypoint indices)
    private final int[][] connections = {
        {5, 7}, {7, 9}, {6, 8}, {8, 10}, // arms
        {5, 6}, {5, 11}, {6, 12}, // shoulders to hips
        {11, 12}, // hips
        {11, 13}, {13, 15}, {12, 14}, {14, 16} // legs
    };
    
    // Colors for keypoints
    private final int[] keypointColors = {
        Color.RED,     // 0 - Nose
        Color.RED,     // 1-4 - Eyes and ears
        Color.RED,
        Color.RED,
        Color.RED,
        Color.YELLOW,  // 5-10 - Shoulders, elbows, wrists
        Color.YELLOW,
        Color.YELLOW,
        Color.YELLOW,
        Color.YELLOW,
        Color.YELLOW,
        Color.GREEN,   // 11-16 - Hips, knees, ankles
        Color.GREEN,
        Color.GREEN,
        Color.GREEN,
        Color.GREEN,
        Color.GREEN
    };
    
    // Constants for optimized drawing
    private static final float POINT_SIZE = 10f;
    private static final float LINE_WIDTH = 4f;
    private static final float MIN_CONFIDENCE = 0.05f; // Lower threshold for more sensitivity

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
        
        // Disable hardware acceleration for more consistent rendering
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setKeypoints(List<float[]> newKeypoints) {
        // Create copy to avoid modification during drawing
        this.keypoints = new ArrayList<>(newKeypoints);
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
        
        // Draw connections first, then points on top
        drawConnections(canvas, width, height);
        drawKeypoints(canvas, width, height);
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
            
            // Set alpha based on confidence
            int avgAlpha = (int)((startPoint[2] + endPoint[2]) * 127);
            linePaint.setColor(Color.CYAN);
            linePaint.setAlpha(avgAlpha);
            
            canvas.drawLine(startX, startY, endX, endY, linePaint);
        }
    }
    
    private void drawKeypoints(Canvas canvas, float width, float height) {
        for (int i = 0; i < keypoints.size(); i++) {
            float[] point = keypoints.get(i);
            float confidence = point[2];
            
            // Skip low confidence keypoints
            if (confidence < MIN_CONFIDENCE) {
                continue;
            }
            
            float x = point[0] * width;
            float y = point[1] * height;
            
            // Set color based on confidence - brighter for higher confidence
            pointPaint.setColor(keypointColors[i]);
            pointPaint.setAlpha(Math.min(255, (int)(confidence * 255 * 1.5f)));
            
            // Draw circle with constant size for better performance
            canvas.drawCircle(x, y, POINT_SIZE, pointPaint);
        }
    }
}