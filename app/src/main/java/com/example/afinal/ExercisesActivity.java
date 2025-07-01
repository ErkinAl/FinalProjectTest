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
            
        // Find the exercise cards
        CardView jumpExerciseCard = findViewById(R.id.jumpExerciseCard);
        CardView armCirclesExerciseCard = findViewById(R.id.armCirclesExerciseCard);
        CardView highKneesExerciseCard = findViewById(R.id.highKneesExerciseCard);
        CardView sideReachExerciseCard = findViewById(R.id.sideReachExerciseCard);
        
        // Apply floating animation for a more playful look
        jumpExerciseCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.floating));
        armCirclesExerciseCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.floating));
        highKneesExerciseCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.floating));
        sideReachExerciseCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.floating));
        
        // Set click listener for the jump exercise card
        jumpExerciseCard.setOnClickListener(v -> {
            // Launch the tutorial activity for jumping
            Intent intent = new Intent(ExercisesActivity.this, TutorialActivity.class);
            intent.putExtra("exercise_type", "jump");
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        
        // Set click listener for the arm circles exercise card
        armCirclesExerciseCard.setOnClickListener(v -> {
            // Launch the tutorial activity for arm circles
            Intent intent = new Intent(ExercisesActivity.this, ArmCirclesTutorialActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        
        // Set click listener for the high knees exercise card
        highKneesExerciseCard.setOnClickListener(v -> {
            // Launch the tutorial activity for high knees
            Intent intent = new Intent(ExercisesActivity.this, HighKneesTutorialActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        
        // Set click listener for the side reach exercise card
        sideReachExerciseCard.setOnClickListener(v -> {
            // Launch the tutorial activity for side reach
            Intent intent = new Intent(ExercisesActivity.this, SideReachTutorialActivity.class);
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