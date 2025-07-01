package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class ShoulderPressTutorialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shoulder_press_tutorial);

        // Initialize views
        ImageView tutorialGif = findViewById(R.id.tutorialGif);
        Button startExerciseButton = findViewById(R.id.startExerciseButton);
        ImageButton backButton = findViewById(R.id.backButton);

        // Set immersive mode for better fullscreen experience
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN);

        // Set back button click listener
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExercisesActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        // Load shoulder press animated GIF using Glide
        loadShoulderPressAnimation(tutorialGif);

        // Set start button click listener
        startExerciseButton.setOnClickListener(v -> startExercise());
    }

    private void loadShoulderPressAnimation(ImageView imageView) {
        // Load the shoulder press GIF from assets using Glide
        try {
            Glide.with(this)
                .asGif()
                .load("file:///android_asset/standingShoulderPress.gif")
                .placeholder(R.drawable.ic_shoulder_press)
                .error(R.drawable.ic_shoulder_press)
                .into(imageView);
        } catch (Exception e) {
            Log.e("ShoulderPressTutorial", "Error loading standingShoulderPress.gif", e);
            // Fallback to static shoulder press icon if GIF loading fails
            imageView.setImageResource(R.drawable.ic_shoulder_press);
        }
    }

    private void startExercise() {
        // Add a nice transition animation
        Button startButton = findViewById(R.id.startExerciseButton);
        startButton.setEnabled(false);
        startButton.setText("Starting...");
        
        // Start the main exercise activity with shoulder press mode
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("fromTutorial", true);
        intent.putExtra("exercise_type", "shoulder_press");
        startActivity(intent);
        
        // Add slide transition
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        
        // Finish this activity so user can't go back to tutorial
        finish();
    }

    @Override
    public void onBackPressed() {
        // Return to exercises activity
        Intent intent = new Intent(this, ExercisesActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-enable the start button if user comes back to this screen
        Button startButton = findViewById(R.id.startExerciseButton);
        startButton.setEnabled(true);
        startButton.setText("🚀 Start Shoulder Press Exercise!");
    }
} 