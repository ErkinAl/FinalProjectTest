using MuvTimeAPI.Models.DTOs;

namespace MuvTimeAPI.Services;

public interface IStatsService
{
    Task<StatsDto> GetUserStatsAsync(string userId);
    Task<StatsDto> UpdateUserStatsAsync(string userId, int jumpsCompleted, int xpEarned, int sessionDuration);
    Task<List<ExerciseSessionDto>> GetUserSessionsAsync(string userId);
    Task<StatsDto> ResetUserStatsAsync(string userId);
    Task<StatsDto> InitializeUserStatsAsync(string userId, string displayName, string email);
} 