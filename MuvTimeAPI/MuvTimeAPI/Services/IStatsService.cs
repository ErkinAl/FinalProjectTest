using MuvTimeAPI.Models.DTOs;

namespace MuvTimeAPI.Services;

public interface IStatsService
{
    Task<StatsDto> GetUserStatsAsync(string userId);
    Task<StatsDto> UpdateUserStatsAsync(string userId, UpdateStatsRequest request);
    Task<List<SessionDto>> GetUserSessionsAsync(string userId, int limit = 10);
    Task ResetUserStatsAsync(string userId);
} 