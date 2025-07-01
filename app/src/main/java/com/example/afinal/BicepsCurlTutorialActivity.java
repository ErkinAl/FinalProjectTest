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

public class BicepsCurlTutorialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biceps_curl_tutorial);

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

        // Load biceps curl animated GIF using Glide
        loadBicepsCurlAnimation(tutorialGif);

        // Set start button click listener
        startExerciseButton.setOnClickListener(v -> startExercise());
    }

    private void loadBicepsCurlAnimation(ImageView imageView) {
        // Load the biceps curl GIF from assets using Glide
        try {
            Glide.with(this)
                .asGif()
                .load("file:///android_asset/bicepsCurl.gif")
                .placeholder(R.drawable.ic_biceps_curl)
                .error(R.drawable.ic_biceps_curl)
                .into(imageView);
        } catch (Exception e) {
            Log.e("BicepsCurlTutorial", "Error loading bicepsCurl.gif", e);
            // Fallback to static biceps curl icon if GIF loading fails
            imageView.setImageResource(R.drawable.ic_biceps_curl);
        }
    }

    private void startExercise() {
        // Add a nice transition animation
        Button startButton = findViewById(R.id.startExerciseButton);
        startButton.setEnabled(false);
        startButton.setText("Starting...");
        
        // Start the main exercise activity with biceps curl mode
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("fromTutorial", true);
        intent.putExtra("exercise_type", "biceps_curl");
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
        startButton.setText("🚀 Start Biceps Curl Exercise!");
    }
} 