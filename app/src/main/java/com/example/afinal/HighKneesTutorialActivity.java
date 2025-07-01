package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class HighKneesTutorialActivity extends AppCompatActivity {
    
    private ImageView highKneesGifView;
    private Button startExerciseButton;
    private ImageButton backButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_high_knees_tutorial);
        
        // Initialize views
        highKneesGifView = findViewById(R.id.highKneesGifView);
        startExerciseButton = findViewById(R.id.startExerciseButton);
        backButton = findViewById(R.id.backButton);
        
        // Set immersive mode for better fullscreen experience
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN);
        
        // Load the high knees animation
        loadHighKneesAnimation();
        
        // Set up click listeners
        startExerciseButton.setOnClickListener(v -> startExercise());
        backButton.setOnClickListener(v -> goBack());
    }
    
    private void loadHighKneesAnimation() {
        // Load the high knees GIF from assets
        try {
            Glide.with(this)
                .asGif()
                .load("file:///android_asset/HighKnee.gif")
                .placeholder(R.drawable.ic_high_knees)
                .error(R.drawable.ic_high_knees)
                .into(highKneesGifView);
        } catch (Exception e) {
            // Fallback to static high knees icon if GIF loading fails
            highKneesGifView.setImageResource(R.drawable.ic_high_knees);
        }
    }
    
    private void startExercise() {
        // Add a nice transition animation
        startExerciseButton.setEnabled(false);
        startExerciseButton.setText("Starting...");
        
        // Start the main exercise activity with high knees mode
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("fromTutorial", true);
        intent.putExtra("exercise_type", "high_knees");
        startActivity(intent);
        
        // Add slide transition
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        
        // Finish this activity so user can't go back to tutorial
        finish();
    }
    
    private void goBack() {
        // Go back to the previous screen (likely ExercisesActivity)
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
    
    @Override
    public void onBackPressed() {
        // Handle back button press
        goBack();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Re-enable the start button if user comes back to this screen
        startExerciseButton.setEnabled(true);
        startExerciseButton.setText("🚀 Start Exercise Now!");
    }
} 