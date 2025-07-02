using Microsoft.AspNetCore.Mvc;
using MuvTimeAPI.Models.DTOs;
using MuvTimeAPI.Services;

namespace MuvTimeAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class StatsController : ControllerBase
{
    private readonly IStatsService _statsService;

    public StatsController(IStatsService statsService)
    {
        _statsService = statsService;
    }

    [HttpGet("{userId}")]
    public async Task<ActionResult<StatsDto>> GetUserStats(string userId)
    {
        try
        {
            var stats = await _statsService.GetUserStatsAsync(userId);
            return Ok(stats);
        }
        catch (Exception ex)
        {
            return StatusCode(500, $"Internal server error: {ex.Message}");
        }
    }

    [HttpPost("{userId}/update")]
    public async Task<ActionResult<StatsDto>> UpdateUserStats(string userId, [FromBody] UpdateStatsRequest request)
    {
        try
        {
            var stats = await _statsService.UpdateUserStatsAsync(userId, request.ExerciseType, request.RepsCompleted, request.XpEarned, request.SessionDuration);
            return Ok(stats);
        }
        catch (Exception ex)
        {
            return StatusCode(500, $"Internal server error: {ex.Message}");
        }
    }

    [HttpGet("{userId}/sessions")]
    public async Task<ActionResult<List<ExerciseSessionDto>>> GetUserSessions(string userId)
    {
        try
        {
            var sessions = await _statsService.GetUserSessionsAsync(userId);
            return Ok(sessions);
        }
        catch (Exception ex)
        {
            return StatusCode(500, $"Internal server error: {ex.Message}");
        }
    }

    [HttpPost("{userId}/reset")]
    public async Task<ActionResult<StatsDto>> ResetUserStats(string userId)
    {
        try
        {
            var stats = await _statsService.ResetUserStatsAsync(userId);
            return Ok(stats);
        }
        catch (Exception ex)
        {
            return StatusCode(500, $"Internal server error: {ex.Message}");
        }
    }

    [HttpPost("{userId}/initialize")]
    public async Task<ActionResult<StatsDto>> InitializeUserStats(string userId, [FromBody] InitializeUserRequest request)
    {
        try
        {
            var stats = await _statsService.InitializeUserStatsAsync(userId, request.DisplayName, request.Email);
            return Ok(stats);
        }
        catch (Exception ex)
        {
            return StatusCode(500, $"Internal server error: {ex.Message}");
        }
    }
}

public class UpdateStatsRequest
{
    public string ExerciseType { get; set; } = "jump";
    public int RepsCompleted { get; set; }
    public int XpEarned { get; set; }
    public int SessionDuration { get; set; }
}

public class InitializeUserRequest
{
    public string DisplayName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
} 