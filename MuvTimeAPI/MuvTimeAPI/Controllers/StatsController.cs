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
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpPost("{userId}/update")]
    public async Task<ActionResult<StatsDto>> UpdateUserStats(string userId, [FromBody] UpdateStatsRequest request)
    {
        try
        {
            var stats = await _statsService.UpdateUserStatsAsync(userId, request);
            return Ok(stats);
        }
        catch (Exception ex)
        {
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpGet("{userId}/sessions")]
    public async Task<ActionResult<List<SessionDto>>> GetUserSessions(string userId, [FromQuery] int limit = 10)
    {
        try
        {
            var sessions = await _statsService.GetUserSessionsAsync(userId, limit);
            return Ok(sessions);
        }
        catch (Exception ex)
        {
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpPost("{userId}/reset")]
    public async Task<ActionResult> ResetUserStats(string userId)
    {
        try
        {
            await _statsService.ResetUserStatsAsync(userId);
            return Ok(new { message = "Stats reset successfully" });
        }
        catch (Exception ex)
        {
            return BadRequest(new { error = ex.Message });
        }
    }
} 