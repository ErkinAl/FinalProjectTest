package com.example.afinal;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiService {
    // Dynamic URL selection based on device type
    private static final String BASE_URL = getApiBaseUrl();
    
    private static String getApiBaseUrl() {
        // Use Azure-deployed API for production
        // Replace with your actual Azure URL after deployment
        return "https://muvtime-api-erkinalkan-fmctdaeygrasavfd.polandcentral-01.azurewebsites.net/api";
        
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
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public ApiService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }
    
    // Data classes for API communication
    public static class UserStats {
        @SerializedName("level")
        public int level;
        
        @SerializedName("xp")
        public int xp;
        
        @SerializedName("totalJumps")
        public int totalJumps;
        
        @SerializedName("exercisesCompleted")
        public int exercisesCompleted;
        
        @SerializedName("xpToNextLevel")
        public int xpToNextLevel;
        
        @SerializedName("currentLevelXp")
        public int currentLevelXp;
    }
    
    public static class UpdateStatsRequest {
        @SerializedName("jumpsCompleted")
        public int jumpsCompleted;
        
        @SerializedName("xpEarned")
        public int xpEarned;
        
        @SerializedName("sessionDuration")
        public int sessionDuration;
        
        public UpdateStatsRequest(int jumpsCompleted, int xpEarned, int sessionDuration) {
            this.jumpsCompleted = jumpsCompleted;
            this.xpEarned = xpEarned;
            this.sessionDuration = sessionDuration;
        }
    }
    
    public static class ExerciseSession {
        @SerializedName("id")
        public String id;
        
        @SerializedName("exerciseType")
        public String exerciseType;
        
        @SerializedName("jumpsCompleted")
        public int jumpsCompleted;
        
        @SerializedName("xpEarned")
        public int xpEarned;
        
        @SerializedName("sessionDuration")
        public int sessionDuration;
        
        @SerializedName("completedAt")
        public String completedAt;
    }
    
    // Get user statistics
    public CompletableFuture<UserStats> getUserStats(String userId) {
        CompletableFuture<UserStats> future = new CompletableFuture<>();
        
        Request request = new Request.Builder()
                .url(BASE_URL + "/stats/" + userId)
                .get()
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    UserStats stats = gson.fromJson(responseBody, UserStats.class);
                    future.complete(stats);
                } else {
                    future.completeExceptionally(new IOException("API Error: " + response.code()));
                }
                response.close();
            }
        });
        
        return future;
    }
    
    // Update user statistics after exercise
    public CompletableFuture<UserStats> updateUserStats(String userId, int jumpsCompleted, int xpEarned, int sessionDuration) {
        CompletableFuture<UserStats> future = new CompletableFuture<>();
        
        UpdateStatsRequest requestBody = new UpdateStatsRequest(jumpsCompleted, xpEarned, sessionDuration);
        String json = gson.toJson(requestBody);
        
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(BASE_URL + "/stats/" + userId + "/update")
                .post(body)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    UserStats stats = gson.fromJson(responseBody, UserStats.class);
                    future.complete(stats);
                } else {
                    future.completeExceptionally(new IOException("API Error: " + response.code()));
                }
                response.close();
            }
        });
        
        return future;
    }
    
    // Get user ID from authenticated session
    public static String getUserId(Context context) {
        String userId = AuthActivity.getCurrentUserId(context);
        if (userId == null || userId.isEmpty()) {
            // Fallback for testing - in production, this should never happen
            return "550e8400-e29b-41d4-a716-446655440000";
        }
        return userId;
    }
} 