using MuvTimeAPI.Models;
using MuvTimeAPI.Models.DTOs;
using Supabase;

namespace MuvTimeAPI.Services;

public class StatsService : IStatsService
{
    private readonly Client _supabaseClient;
    private const int XP_PER_LEVEL = 100;

    public StatsService(Client supabaseClient)
    {
        _supabaseClient = supabaseClient;
    }

    public async Task<StatsDto> GetUserStatsAsync(string userId)
    {
        // Get or create user stats
        var userStats = await GetOrCreateUserStatsAsync(userId);
        
        return new StatsDto
        {
            Level = userStats.Level,
            Xp = userStats.Xp,
            TotalJumps = userStats.TotalJumps,
            TotalArmCircles = userStats.TotalArmCircles,
            TotalHighKnees = userStats.TotalHighKnees,
            TotalSideReaches = userStats.TotalSideReaches,
            TotalJackJumps = userStats.TotalJackJumps,
            TotalBicepsCurls = userStats.TotalBicepsCurls,
            TotalShoulderPresses = userStats.TotalShoulderPresses,
            TotalSquats = userStats.TotalSquats,
            ExercisesCompleted = userStats.ExercisesCompleted,
            TotalAllExercises = userStats.TotalJumps + userStats.TotalArmCircles + userStats.TotalHighKnees + 
                               userStats.TotalSideReaches + userStats.TotalJackJumps + userStats.TotalBicepsCurls + 
                               userStats.TotalShoulderPresses + userStats.TotalSquats,
            XpToNextLevel = CalculateXpToNextLevel(userStats.Xp),
            CurrentLevelXp = CalculateCurrentLevelXp(userStats.Xp)
        };
    }

    public async Task<StatsDto> UpdateUserStatsAsync(string userId, string exerciseType, int repsCompleted, int xpEarned, int sessionDuration)
    {
        var userStats = await GetOrCreateUserStatsAsync(userId);
        
        // Update stats based on exercise type
        switch (exerciseType.ToLower())
        {
            case "jump":
                userStats.TotalJumps += repsCompleted;
                break;
            case "arm_circles":
                userStats.TotalArmCircles += repsCompleted;
                break;
            case "high_knees":
                userStats.TotalHighKnees += repsCompleted;
                break;
            case "side_reach":
                userStats.TotalSideReaches += repsCompleted;
                break;
            case "jack_jumps":
                userStats.TotalJackJumps += repsCompleted;
                break;
            case "biceps_curl":
                userStats.TotalBicepsCurls += repsCompleted;
                break;
            case "shoulder_press":
                userStats.TotalShoulderPresses += repsCompleted;
                break;
            case "squat":
                userStats.TotalSquats += repsCompleted;
                break;
            default:
                userStats.TotalJumps += repsCompleted; // Default to jumps
                break;
        }
        
        userStats.ExercisesCompleted += 1;
        userStats.Xp += xpEarned;
        userStats.Level = CalculateLevel(userStats.Xp);
        userStats.UpdatedAt = DateTime.UtcNow;

        // Save updated stats
        await _supabaseClient.From<UserStats>()
            .Update(userStats);

        // Create exercise session record
        var session = new ExerciseSession
        {
            Id = Guid.NewGuid().ToString(),
            UserId = userId,
            ExerciseType = exerciseType,
            RepsCompleted = repsCompleted,
            XpEarned = xpEarned,
            SessionDuration = sessionDuration,
            CompletedAt = DateTime.UtcNow
        };

        await _supabaseClient.From<ExerciseSession>()
            .Insert(session);

        return new StatsDto
        {
            Level = userStats.Level,
            Xp = userStats.Xp,
            TotalJumps = userStats.TotalJumps,
            TotalArmCircles = userStats.TotalArmCircles,
            TotalHighKnees = userStats.TotalHighKnees,
            TotalSideReaches = userStats.TotalSideReaches,
            TotalJackJumps = userStats.TotalJackJumps,
            TotalBicepsCurls = userStats.TotalBicepsCurls,
            TotalShoulderPresses = userStats.TotalShoulderPresses,
            TotalSquats = userStats.TotalSquats,
            ExercisesCompleted = userStats.ExercisesCompleted,
            TotalAllExercises = userStats.TotalJumps + userStats.TotalArmCircles + userStats.TotalHighKnees + 
                               userStats.TotalSideReaches + userStats.TotalJackJumps + userStats.TotalBicepsCurls + 
                               userStats.TotalShoulderPresses + userStats.TotalSquats,
            XpToNextLevel = CalculateXpToNextLevel(userStats.Xp),
            CurrentLevelXp = CalculateCurrentLevelXp(userStats.Xp)
        };
    }

    public async Task<List<ExerciseSessionDto>> GetUserSessionsAsync(string userId)
    {
        var sessions = await _supabaseClient.From<ExerciseSession>()
            .Where(s => s.UserId == userId)
            .Order("completed_at", Supabase.Postgrest.Constants.Ordering.Descending)
            .Limit(10)
            .Get();

        return sessions.Models.Select(s => new ExerciseSessionDto
        {
            Id = s.Id,
            ExerciseType = s.ExerciseType,
            RepsCompleted = s.RepsCompleted,
            XpEarned = s.XpEarned,
            SessionDuration = s.SessionDuration,
            CompletedAt = s.CompletedAt
        }).ToList();
    }

    public async Task<StatsDto> ResetUserStatsAsync(string userId)
    {
        var userStats = await GetOrCreateUserStatsAsync(userId);
        
        userStats.Xp = 0;
        userStats.Level = 0;
        userStats.TotalJumps = 0;
        userStats.TotalArmCircles = 0;
        userStats.TotalHighKnees = 0;
        userStats.TotalSideReaches = 0;
        userStats.TotalJackJumps = 0;
        userStats.TotalBicepsCurls = 0;
        userStats.TotalShoulderPresses = 0;
        userStats.TotalSquats = 0;
        userStats.ExercisesCompleted = 0;
        userStats.UpdatedAt = DateTime.UtcNow;

        await _supabaseClient.From<UserStats>()
            .Update(userStats);

        return new StatsDto
        {
            Level = userStats.Level,
            Xp = userStats.Xp,
            TotalJumps = userStats.TotalJumps,
            TotalArmCircles = userStats.TotalArmCircles,
            TotalHighKnees = userStats.TotalHighKnees,
            TotalSideReaches = userStats.TotalSideReaches,
            TotalJackJumps = userStats.TotalJackJumps,
            TotalBicepsCurls = userStats.TotalBicepsCurls,
            TotalShoulderPresses = userStats.TotalShoulderPresses,
            TotalSquats = userStats.TotalSquats,
            ExercisesCompleted = userStats.ExercisesCompleted,
            TotalAllExercises = userStats.TotalJumps + userStats.TotalArmCircles + userStats.TotalHighKnees + 
                               userStats.TotalSideReaches + userStats.TotalJackJumps + userStats.TotalBicepsCurls + 
                               userStats.TotalShoulderPresses + userStats.TotalSquats,
            XpToNextLevel = CalculateXpToNextLevel(userStats.Xp),
            CurrentLevelXp = CalculateCurrentLevelXp(userStats.Xp)
        };
    }

    public async Task<StatsDto> InitializeUserStatsAsync(string userId, string displayName, string email)
    {
        // Check if user profile exists, create if not
        var profileResponse = await _supabaseClient.From<UserProfile>()
            .Where(p => p.Id == userId)
            .Get();

        if (profileResponse.Models.Count == 0)
        {
            var newProfile = new UserProfile
            {
                Id = userId,
                DisplayName = displayName,
                Email = email,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            await _supabaseClient.From<UserProfile>()
                .Insert(newProfile);
        }

        // Get or create user stats
        var userStats = await GetOrCreateUserStatsAsync(userId);

        return new StatsDto
        {
            Level = userStats.Level,
            Xp = userStats.Xp,
            TotalJumps = userStats.TotalJumps,
            TotalArmCircles = userStats.TotalArmCircles,
            TotalHighKnees = userStats.TotalHighKnees,
            TotalSideReaches = userStats.TotalSideReaches,
            TotalJackJumps = userStats.TotalJackJumps,
            TotalBicepsCurls = userStats.TotalBicepsCurls,
            TotalShoulderPresses = userStats.TotalShoulderPresses,
            TotalSquats = userStats.TotalSquats,
            ExercisesCompleted = userStats.ExercisesCompleted,
            TotalAllExercises = userStats.TotalJumps + userStats.TotalArmCircles + userStats.TotalHighKnees + 
                               userStats.TotalSideReaches + userStats.TotalJackJumps + userStats.TotalBicepsCurls + 
                               userStats.TotalShoulderPresses + userStats.TotalSquats,
            XpToNextLevel = CalculateXpToNextLevel(userStats.Xp),
            CurrentLevelXp = CalculateCurrentLevelXp(userStats.Xp)
        };
    }

    private async Task<UserStats> GetOrCreateUserStatsAsync(string userId)
    {
        var response = await _supabaseClient.From<UserStats>()
            .Where(s => s.UserId == userId)
            .Get();

        if (response.Models.Count > 0)
        {
            return response.Models.First();
        }

        // Create new user stats
        var newStats = new UserStats
        {
            Id = Guid.NewGuid().ToString(),
            UserId = userId,
            Xp = 0,
            Level = 0,
            TotalJumps = 0,
            TotalArmCircles = 0,
            TotalHighKnees = 0,
            TotalSideReaches = 0,
            TotalJackJumps = 0,
            TotalBicepsCurls = 0,
            TotalShoulderPresses = 0,
            TotalSquats = 0,
            ExercisesCompleted = 0,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        await _supabaseClient.From<UserStats>()
            .Insert(newStats);

        return newStats;
    }

    private int CalculateLevel(int xp)
    {
        return xp / XP_PER_LEVEL;
    }

    private int CalculateXpToNextLevel(int xp)
    {
        var currentLevelXp = xp % XP_PER_LEVEL;
        return XP_PER_LEVEL - currentLevelXp;
    }

    private int CalculateCurrentLevelXp(int xp)
    {
        return xp % XP_PER_LEVEL;
    }
} 