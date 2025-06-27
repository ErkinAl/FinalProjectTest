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
            ExercisesCompleted = userStats.ExercisesCompleted,
            XpToNextLevel = CalculateXpToNextLevel(userStats.Xp),
            CurrentLevelXp = CalculateCurrentLevelXp(userStats.Xp)
        };
    }

    public async Task<StatsDto> UpdateUserStatsAsync(string userId, int jumpsCompleted, int xpEarned, int sessionDuration)
    {
        var userStats = await GetOrCreateUserStatsAsync(userId);
        
        // Update stats
        userStats.TotalJumps += jumpsCompleted;
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
            ExerciseType = "jump_counter",
            JumpsCompleted = jumpsCompleted,
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
            ExercisesCompleted = userStats.ExercisesCompleted,
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
            JumpsCompleted = s.JumpsCompleted,
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
        userStats.ExercisesCompleted = 0;
        userStats.UpdatedAt = DateTime.UtcNow;

        await _supabaseClient.From<UserStats>()
            .Update(userStats);

        return new StatsDto
        {
            Level = userStats.Level,
            Xp = userStats.Xp,
            TotalJumps = userStats.TotalJumps,
            ExercisesCompleted = userStats.ExercisesCompleted,
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
            ExercisesCompleted = userStats.ExercisesCompleted,
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