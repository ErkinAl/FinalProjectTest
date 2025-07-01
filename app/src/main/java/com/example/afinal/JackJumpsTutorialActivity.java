package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class JackJumpsTutorialActivity extends AppCompatActivity {
    
    private ImageView jackJumpsGifView;
    private Button startExerciseButton;
    private ImageButton backButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jack_jumps_tutorial);
        
        // Initialize views
        jackJumpsGifView = findViewById(R.id.jackJumpsGifView);
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
        
        // Load the jack jumps animation
        loadJackJumpsAnimation();
        
        // Set up click listeners
        startExerciseButton.setOnClickListener(v -> startExercise());
        backButton.setOnClickListener(v -> goBack());
    }
    
    private void loadJackJumpsAnimation() {
        // Load the jack jumps GIF from assets
        try {
            Glide.with(this)
                .asGif()
                .load("file:///android_asset/jackJumps.gif")
                .placeholder(R.drawable.ic_jack_jumps)
                .error(R.drawable.ic_jack_jumps)
                .into(jackJumpsGifView);
        } catch (Exception e) {
            // Fallback to static jack jumps icon if GIF loading fails
            jackJumpsGifView.setImageResource(R.drawable.ic_jack_jumps);
        }
    }
    
    private void startExercise() {
        // Add a nice transition animation
        startExerciseButton.setEnabled(false);
        startExerciseButton.setText("Starting...");
        
        // Start the main exercise activity with jack jumps mode
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("fromTutorial", true);
        intent.putExtra("exercise_type", "jack_jumps");
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