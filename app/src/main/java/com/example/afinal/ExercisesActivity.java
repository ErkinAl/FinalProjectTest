package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ExercisesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercises);
        
        // Set immersive sticky mode for better fullscreen experience
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN);
            
        // Find the jump exercise card
        CardView jumpExerciseCard = findViewById(R.id.jumpExerciseCard);
        
        // Apply floating animation for a more playful look
        jumpExerciseCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.floating));
        
        // Set click listener for the jump exercise card
        jumpExerciseCard.setOnClickListener(v -> {
            // Launch the tutorial activity first
            Intent intent = new Intent(ExercisesActivity.this, TutorialActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        
        // Set up bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_exercises); // Highlight exercises tab
        
        // Handle navigation selection
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_exercises) {
                // Already on exercises screen
                return true;
            } else if (itemId == R.id.nav_stats) {
                // Navigate to stats
                startActivity(new Intent(this, StatsActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            }
            return false;
        });
    }
    
    @Override
    public void onBackPressed() {
        // Return to starter activity with animation
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
} 