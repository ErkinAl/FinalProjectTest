package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class StarterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starter);
        
        // Hide the status bar for a more immersive experience
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | 
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | 
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        
        // Find the play button
        ImageButton playButton = findViewById(R.id.playButton);
        
        // Apply pulsing animation
        playButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
        
        // Set click listener to start the exercises activity
        playButton.setOnClickListener(v -> {
            // Stop animation when clicked
            playButton.clearAnimation();
            
            Intent intent = new Intent(StarterActivity.this, ExercisesActivity.class);
            startActivity(intent);
            
            // Apply transition animation
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }
} 