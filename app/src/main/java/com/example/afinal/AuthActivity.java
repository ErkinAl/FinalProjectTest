package com.example.afinal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etDisplayName;
    private Button btnLogin, btnRegister, btnSwitchMode;
    private TextView tvTitle, tvSwitchText;
    private ProgressBar progressBar;
    private View displayNameContainer;
    
    private boolean isLoginMode = true;
    private OkHttpClient httpClient;
    private Gson gson;
    
    private static final String SUPABASE_URL = "https://hatrwnyeviroiwwdisht.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhhdHJ3bnlldmlyb2l3d2Rpc2h0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTA5NzIyMTksImV4cCI6MjA2NjU0ODIxOX0.0Evm7MPiftiTm-Ot8LUXZ8jhIJWXQieHKYUDgmggrnk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        
        // Set immersive mode
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN);
        
        initializeViews();
        setupClickListeners();
        
        httpClient = new OkHttpClient();
        gson = new Gson();
        
        // Check if user is already logged in
        if (isUserLoggedIn()) {
            proceedToMainApp();
        }
    }
    
    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etDisplayName = findViewById(R.id.etDisplayName);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnSwitchMode = findViewById(R.id.btnSwitchMode);
        tvTitle = findViewById(R.id.tvTitle);
        tvSwitchText = findViewById(R.id.tvSwitchText);
        progressBar = findViewById(R.id.progressBar);
        displayNameContainer = findViewById(R.id.displayNameContainer);
        
        updateUIForMode();
    }
    
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            if (isLoginMode) {
                performLogin();
            } else {
                performRegister();
            }
        });
        
        btnRegister.setOnClickListener(v -> {
            if (isLoginMode) {
                performLogin();
            } else {
                performRegister();
            }
        });
        
        btnSwitchMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUIForMode();
        });
    }
    
    private void updateUIForMode() {
        if (isLoginMode) {
            tvTitle.setText("Welcome Back!");
            btnLogin.setText("LOGIN");
            btnRegister.setText("LOGIN");
            btnSwitchMode.setText("CREATE ACCOUNT");
            tvSwitchText.setText("Don't have an account?");
            displayNameContainer.setVisibility(View.GONE);
        } else {
            tvTitle.setText("Join MuvTime!");
            btnLogin.setText("REGISTER");
            btnRegister.setText("REGISTER");
            btnSwitchMode.setText("SIGN IN");
            tvSwitchText.setText("Already have an account?");
            displayNameContainer.setVisibility(View.VISIBLE);
        }
    }
    
    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (!validateInput(email, password, null)) {
            return;
        }
        
        setLoading(true);
        
        loginUser(email, password)
            .thenAccept(success -> {
                runOnUiThread(() -> {
                    setLoading(false);
                    if (success) {
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        proceedToMainApp();
                    } else {
                        Toast.makeText(this, "Login failed. Please check your credentials.", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Login error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                });
                return null;
            });
    }
    
    private void performRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String displayName = etDisplayName.getText().toString().trim();
        
        if (!validateInput(email, password, displayName)) {
            return;
        }
        
        setLoading(true);
        
        registerUser(email, password, displayName)
            .thenAccept(success -> {
                runOnUiThread(() -> {
                    setLoading(false);
                    if (success) {
                        Toast.makeText(this, "Registration successful! Please check your email to verify your account.", Toast.LENGTH_LONG).show();
                        // Switch to login mode after successful registration
                        isLoginMode = true;
                        updateUIForMode();
                    } else {
                        Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Registration error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                });
                return null;
            });
    }
    
    private boolean validateInput(String email, String password, String displayName) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return false;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            return false;
        }
        
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return false;
        }
        
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return false;
        }
        
        if (!isLoginMode && TextUtils.isEmpty(displayName)) {
            etDisplayName.setError("Display name is required");
            return false;
        }
        
        return true;
    }
    
    private CompletableFuture<Boolean> loginUser(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject loginData = new JsonObject();
                loginData.addProperty("email", email);
                loginData.addProperty("password", password);
                
                RequestBody body = RequestBody.create(
                    gson.toJson(loginData),
                    MediaType.get("application/json")
                );
                
                Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/auth/v1/token?grant_type=password")
                    .post(body)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonObject authResponse = gson.fromJson(responseBody, JsonObject.class);
                        
                        // Save user session
                        saveUserSession(authResponse);
                        
                        // Initialize user stats in database
                        initializeUserStats(authResponse);
                        
                        return true;
                    }
                }
                return false;
            } catch (Exception e) {
                Log.e("AuthActivity", "Login error", e);
                return false;
            }
        });
    }
    
    private CompletableFuture<Boolean> registerUser(String email, String password, String displayName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject signupData = new JsonObject();
                signupData.addProperty("email", email);
                signupData.addProperty("password", password);
                
                JsonObject userData = new JsonObject();
                userData.addProperty("display_name", displayName);
                signupData.add("data", userData);
                
                RequestBody body = RequestBody.create(
                    gson.toJson(signupData),
                    MediaType.get("application/json")
                );
                
                Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/auth/v1/signup")
                    .post(body)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        Log.d("AuthActivity", "Registration response: " + responseBody);
                        return true;
                    }
                }
                return false;
            } catch (Exception e) {
                Log.e("AuthActivity", "Registration error", e);
                return false;
            }
        });
    }
    
    private void saveUserSession(JsonObject authResponse) {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        if (authResponse.has("access_token")) {
            editor.putString("access_token", authResponse.get("access_token").getAsString());
        }
        
        if (authResponse.has("user")) {
            JsonObject user = authResponse.getAsJsonObject("user");
            if (user.has("id")) {
                editor.putString("user_id", user.get("id").getAsString());
            }
            if (user.has("email")) {
                editor.putString("user_email", user.get("email").getAsString());
            }
        }
        
        editor.putBoolean("is_logged_in", true);
        editor.apply();
    }
    
    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        return prefs.getBoolean("is_logged_in", false) && 
               !TextUtils.isEmpty(prefs.getString("user_id", ""));
    }
    
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
        btnSwitchMode.setEnabled(!loading);
    }
    
    private void proceedToMainApp() {
        Intent intent = new Intent(this, ExercisesActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
    
    public static String getCurrentUserId(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE);
        return prefs.getString("user_id", null);
    }
    
    public static void logout(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
    
    private static String getApiBaseUrl() {
        // Use Azure-deployed API for production
        // Replace with your actual Azure URL after deployment
        return "https://your-app-name.azurewebsites.net/api";
        
        // Uncomment below for local development
        /*
        String fingerprint = android.os.Build.FINGERPRINT;
        if (fingerprint.contains("generic") || fingerprint.contains("emulator")) {
            return "http://10.0.2.2:5129/api";
        } else {
            return "http://192.168.1.156:5129/api";
        }
        */
    }
    
    private void initializeUserStats(JsonObject authResponse) {
        // Initialize user stats in background thread
        CompletableFuture.runAsync(() -> {
            try {
                if (authResponse.has("user")) {
                    JsonObject user = authResponse.getAsJsonObject("user");
                    String userId = user.get("id").getAsString();
                    String email = user.get("email").getAsString();
                    
                    // Get display name from user metadata if available
                    String displayName = email.split("@")[0]; // Default to email prefix
                    if (user.has("user_metadata")) {
                        JsonObject metadata = user.getAsJsonObject("user_metadata");
                        if (metadata.has("display_name")) {
                            displayName = metadata.get("display_name").getAsString();
                        }
                    }
                    
                    // Call API to initialize user stats
                    JsonObject initData = new JsonObject();
                    initData.addProperty("displayName", displayName);
                    initData.addProperty("email", email);
                    
                    RequestBody body = RequestBody.create(
                        gson.toJson(initData),
                        MediaType.get("application/json")
                    );
                    
                    String apiUrl = getApiBaseUrl() + "/stats/" + userId + "/initialize";
                    Request request = new Request.Builder()
                        .url(apiUrl)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();
                    
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            Log.d("AuthActivity", "User stats initialized successfully");
                        } else {
                            Log.w("AuthActivity", "Failed to initialize user stats: " + response.code());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("AuthActivity", "Error initializing user stats", e);
            }
        });
    }
} 