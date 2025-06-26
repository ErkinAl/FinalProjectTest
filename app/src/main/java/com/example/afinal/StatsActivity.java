package com.example.afinal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class StatsActivity extends AppCompatActivity {

    private TextView tvXpValue;
    private TextView tvJumpCountValue;
    private TextView tvExercisesCompletedValue;
    private TextView tvLevelValue;
    private ProgressBar pbNextLevel;
    private TextView tvNextLevelProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        
        // Set immersive sticky mode for better fullscreen experience
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN);
            
        // Initialize views
        tvXpValue = findViewById(R.id.tvXpValue);
        tvJumpCountValue = findViewById(R.id.tvJumpCountValue);
        tvExercisesCompletedValue = findViewById(R.id.tvExercisesCompletedValue);
        tvLevelValue = findViewById(R.id.tvLevelValue);
        pbNextLevel = findViewById(R.id.pbNextLevel);
        tvNextLevelProgress = findViewById(R.id.tvNextLevelProgress);
        
        // Apply animations to cards
        applyAnimations();
        
        // Set up bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_stats); // Highlight stats tab
        
        // Handle navigation selection
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_exercises) {
                // Navigate to exercises
                startActivity(new Intent(this, ExercisesActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (itemId == R.id.nav_stats) {
                // Already on stats screen
                return true;
            }
            return false;
        });
        
        // Load stats from SharedPreferences
        loadStats();
    }
    
    private void applyAnimations() {
        // Apply pulsing animation to progress bar
        Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.progress_pulse);
        pbNextLevel.startAnimation(pulseAnimation);
        
        // Apply staggered entry animations to value text views
        tvLevelValue.setAlpha(0f);
        tvLevelValue.animate().alpha(1f).setDuration(800).setStartDelay(300);
        
        tvXpValue.setAlpha(0f);
        tvXpValue.animate().alpha(1f).setDuration(600).setStartDelay(500);
        
        tvJumpCountValue.setAlpha(0f);
        tvJumpCountValue.animate().alpha(1f).setDuration(600).setStartDelay(700);
        
        tvExercisesCompletedValue.setAlpha(0f);
        tvExercisesCompletedValue.animate().alpha(1f).setDuration(600).setStartDelay(900);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh stats when activity resumes
        loadStats();
    }
    
    private void loadStats() {
        SharedPreferences prefs = getSharedPreferences("user_stats", MODE_PRIVATE);
        
        // Reset all stats to 0 for new users (temporary - remove this when backend is ready)
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("xp", 0);
        editor.putInt("jump_count", 0);
        editor.putInt("exercises_completed", 0);
        editor.apply();
        
        // Get saved stats, defaulting to 0 if not found
        int xp = prefs.getInt("xp", 0);
        int jumpCount = prefs.getInt("jump_count", 0);
        int exercisesCompleted = prefs.getInt("exercises_completed", 0);
        
        // Calculate level (starting at level 0, level up every 100 XP)
        int level = xp / 100;  // Level 0 at 0-99 XP, Level 1 at 100-199 XP, etc.
        int progressToNextLevel = xp % 100;  // XP progress within current level
        
        // Set values directly to ensure they show up
        tvXpValue.setText(String.valueOf(xp));
        tvJumpCountValue.setText(String.valueOf(jumpCount));
        tvExercisesCompletedValue.setText(String.valueOf(exercisesCompleted));
        tvLevelValue.setText(String.valueOf(level));
        
        // Set progress bar and text
        pbNextLevel.setProgress(progressToNextLevel);
        tvNextLevelProgress.setText(progressToNextLevel + "/100");
    }
    

} 