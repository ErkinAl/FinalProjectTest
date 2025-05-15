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
        
        // Get saved stats, defaulting to 0 if not found
        int xp = prefs.getInt("xp", 0);
        int jumpCount = prefs.getInt("jump_count", 0);
        int exercisesCompleted = prefs.getInt("exercises_completed", 0);
        
        // Calculate level (1 level per 100 XP)
        int level = (xp / 100) + 1;
        int xpToNextLevel = 100 - (xp % 100);
        int progressToNextLevel = xp % 100;
        
        // Update UI with fun animations
        animateTextChange(tvXpValue, xp);
        animateTextChange(tvJumpCountValue, jumpCount);
        animateTextChange(tvExercisesCompletedValue, exercisesCompleted);
        animateTextChange(tvLevelValue, level);
        
        // Animate progress bar
        pbNextLevel.setProgress(0);
        pbNextLevel.animate()
            .setDuration(1000)
            .setStartDelay(500)
            .withEndAction(() -> {
                pbNextLevel.setProgress(progressToNextLevel);
                tvNextLevelProgress.setText(progressToNextLevel + "/100");
            });
    }
    
    // Helper method to animate number changes
    private void animateTextChange(final TextView textView, final int newValue) {
        try {
            // Get current value
            int currentValue = Integer.parseInt(textView.getText().toString());
            
            // Don't animate if values are the same
            if (currentValue == newValue) {
                return;
            }
            
            // Determine if counting up or down
            final boolean countUp = newValue > currentValue;
            
            // Determine increment based on difference
            final int increment = Math.max(1, Math.abs(newValue - currentValue) / 20);
            
            // Start animation
            textView.animate().cancel();
            textView.setScaleX(1.0f);
            textView.setScaleY(1.0f);
            
            Runnable updateTextRunnable = new Runnable() {
                int currentCount = currentValue;
                
                @Override
                public void run() {
                    if ((countUp && currentCount < newValue) || (!countUp && currentCount > newValue)) {
                        // Update the counter value
                        currentCount = countUp ? Math.min(newValue, currentCount + increment) : 
                                                Math.max(newValue, currentCount - increment);
                        textView.setText(String.valueOf(currentCount));
                        
                        // Add a small scale animation with each change
                        textView.animate()
                            .scaleX(1.1f)
                            .scaleY(1.1f)
                            .setDuration(50)
                            .withEndAction(() -> 
                                textView.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(50)
                                    .start()
                            )
                            .start();
                        
                        // Continue updating
                        textView.postDelayed(this, 50);
                    } else {
                        // Ensure final value is correct
                        textView.setText(String.valueOf(newValue));
                        
                        // Final celebration animation
                        textView.animate()
                            .scaleX(1.3f)
                            .scaleY(1.3f)
                            .setDuration(200)
                            .withEndAction(() -> 
                                textView.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(200)
                                    .start()
                            )
                            .start();
                    }
                }
            };
            
            // Start the update process
            textView.post(updateTextRunnable);
            
        } catch (NumberFormatException e) {
            // Handle case where text isn't a number
            textView.setText(String.valueOf(newValue));
        }
    }
} 