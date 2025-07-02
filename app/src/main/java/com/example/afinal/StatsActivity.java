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
    private TextView tvArmCirclesValue;
    private TextView tvHighKneesValue;
    private TextView tvSideReachesValue;
    private TextView tvJackJumpsValue;
    private TextView tvBicepsCurlsValue;
    private TextView tvShoulderPressValue;
    private TextView tvSquatsValue;
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
        tvArmCirclesValue = findViewById(R.id.tvArmCirclesValue);
        tvHighKneesValue = findViewById(R.id.tvHighKneesValue);
        tvSideReachesValue = findViewById(R.id.tvSideReachesValue);
        tvJackJumpsValue = findViewById(R.id.tvJackJumpsValue);
        tvBicepsCurlsValue = findViewById(R.id.tvBicepsCurlsValue);
        tvShoulderPressValue = findViewById(R.id.tvShoulderPressValue);
        tvSquatsValue = findViewById(R.id.tvSquatsValue);
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
                // Refresh stats when stats tab is tapped
                loadStats();
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
        
        tvArmCirclesValue.setAlpha(0f);
        tvArmCirclesValue.animate().alpha(1f).setDuration(600).setStartDelay(800);
        
        tvHighKneesValue.setAlpha(0f);
        tvHighKneesValue.animate().alpha(1f).setDuration(600).setStartDelay(900);
        
        tvSideReachesValue.setAlpha(0f);
        tvSideReachesValue.animate().alpha(1f).setDuration(600).setStartDelay(1000);
        
        tvJackJumpsValue.setAlpha(0f);
        tvJackJumpsValue.animate().alpha(1f).setDuration(600).setStartDelay(1100);
        
        tvBicepsCurlsValue.setAlpha(0f);
        tvBicepsCurlsValue.animate().alpha(1f).setDuration(600).setStartDelay(1200);
        
        tvShoulderPressValue.setAlpha(0f);
        tvShoulderPressValue.animate().alpha(1f).setDuration(600).setStartDelay(1300);
        
        tvSquatsValue.setAlpha(0f);
        tvSquatsValue.animate().alpha(1f).setDuration(600).setStartDelay(1400);
        
        tvExercisesCompletedValue.setAlpha(0f);
        tvExercisesCompletedValue.animate().alpha(1f).setDuration(600).setStartDelay(1500);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh stats when activity resumes
        loadStats();
    }
    
    private void loadStats() {
        // Always prioritize database data - clear local cache first
        clearLocalCache();
        
        // Show loading state
        showLoadingState();
        
        // Load stats from API (database)
        ApiService apiService = new ApiService();
        String userId = ApiService.getUserId(this);
        
        android.util.Log.d("StatsActivity", "Loading stats for user ID: " + userId);
        
        apiService.getUserStats(userId)
            .thenAccept(stats -> {
                runOnUiThread(() -> {
                    android.util.Log.d("StatsActivity", "API Response - Level: " + stats.level + ", XP: " + stats.xp + ", Total All Exercises: " + stats.totalAllExercises);
        
                    // Update UI with fresh database data - pass all exercise types
                    updateStatsDisplay(stats);
        
                    // Update local storage with latest database data
                    SharedPreferences prefs = getSharedPreferences("user_stats", MODE_PRIVATE);
                    prefs.edit()
                        .putInt("xp", stats.xp)
                        .putInt("level", stats.level)
                        .putInt("jump_count", stats.totalJumps)
                        .putInt("arm_circles_count", stats.totalArmCircles)
                        .putInt("high_knees_count", stats.totalHighKnees)
                        .putInt("side_reaches_count", stats.totalSideReaches)
                        .putInt("jack_jumps_count", stats.totalJackJumps)
                        .putInt("biceps_curls_count", stats.totalBicepsCurls)
                        .putInt("shoulder_presses_count", stats.totalShoulderPresses)
                        .putInt("squats_count", stats.totalSquats)
                        .putInt("exercises_completed", stats.exercisesCompleted)
                        .apply();
                    
                    hideLoadingState();
            });
            })
            .exceptionally(throwable -> {
                // API call failed - check the specific error
                runOnUiThread(() -> {
                    String errorMessage = throwable.getMessage();
                    android.util.Log.e("StatsActivity", "API failed: " + errorMessage, throwable);
                    
                    // Check if it's a network connectivity issue or API server issue
                    if (errorMessage != null && (errorMessage.contains("ConnectException") || 
                                                errorMessage.contains("UnknownHostException") ||
                                                errorMessage.contains("SocketTimeoutException"))) {
                        // Network/server connection issue
                        android.widget.Toast.makeText(this, "Cannot connect to server - using cached data", android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        // Other API error - don't show offline message since we have internet
                        android.widget.Toast.makeText(this, "Loading stats...", android.widget.Toast.LENGTH_SHORT).show();
                    }
                    
                    SharedPreferences prefs = getSharedPreferences("user_stats", MODE_PRIVATE);
                    
                    // Create a stats object from local storage
                    ApiService.UserStats localStats = new ApiService.UserStats();
                    localStats.xp = prefs.getInt("xp", 0);
                    localStats.level = prefs.getInt("level", 0);
                    localStats.totalJumps = prefs.getInt("jump_count", 0);
                    localStats.totalArmCircles = prefs.getInt("arm_circles_count", 0);
                    localStats.totalHighKnees = prefs.getInt("high_knees_count", 0);
                    localStats.totalSideReaches = prefs.getInt("side_reaches_count", 0);
                    localStats.totalJackJumps = prefs.getInt("jack_jumps_count", 0);
                    localStats.totalBicepsCurls = prefs.getInt("biceps_curls_count", 0);
                    localStats.totalShoulderPresses = prefs.getInt("shoulder_presses_count", 0);
                    localStats.totalSquats = prefs.getInt("squats_count", 0);
                    localStats.exercisesCompleted = prefs.getInt("exercises_completed", 0);
                    localStats.currentLevelXp = localStats.xp % 100;
                    
                    updateStatsDisplay(localStats);
                    hideLoadingState();
                });
                return null;
            });
    }
    
    private void clearLocalCache() {
        // Clear old local data to ensure we show fresh database data
        SharedPreferences prefs = getSharedPreferences("user_stats", MODE_PRIVATE);
        prefs.edit().clear().apply();
        android.util.Log.d("StatsActivity", "Local cache cleared");
    }
    
    private void showLoadingState() {
        // Show loading indicators
        tvXpValue.setText("...");
        tvJumpCountValue.setText("...");
        tvArmCirclesValue.setText("...");
        tvHighKneesValue.setText("...");
        tvSideReachesValue.setText("...");
        tvJackJumpsValue.setText("...");
        tvBicepsCurlsValue.setText("...");
        tvShoulderPressValue.setText("...");
        tvSquatsValue.setText("...");
        tvExercisesCompletedValue.setText("...");
        tvLevelValue.setText("...");
        tvNextLevelProgress.setText("Loading...");
    }
    
    private void hideLoadingState() {
        // Loading complete - UI already updated with real data
    }
    
    private void updateStatsDisplay(ApiService.UserStats stats) {
        tvXpValue.setText(String.valueOf(stats.xp));
        tvJumpCountValue.setText(String.valueOf(stats.totalJumps));
        tvArmCirclesValue.setText(String.valueOf(stats.totalArmCircles));
        tvHighKneesValue.setText(String.valueOf(stats.totalHighKnees));
        tvSideReachesValue.setText(String.valueOf(stats.totalSideReaches));
        tvJackJumpsValue.setText(String.valueOf(stats.totalJackJumps));
        tvBicepsCurlsValue.setText(String.valueOf(stats.totalBicepsCurls));
        tvShoulderPressValue.setText(String.valueOf(stats.totalShoulderPresses));
        tvSquatsValue.setText(String.valueOf(stats.totalSquats));
        tvExercisesCompletedValue.setText(String.valueOf(stats.exercisesCompleted));
        tvLevelValue.setText(String.valueOf(stats.level));
        
        // Set progress bar and text
        pbNextLevel.setProgress(stats.currentLevelXp);
        tvNextLevelProgress.setText(stats.currentLevelXp + "/100");
        }
    

} 