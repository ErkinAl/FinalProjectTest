using Microsoft.AspNetCore.Mvc;
using MuvTimeAPI.Services;

namespace MuvTimeAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class TestController : ControllerBase
{
    private readonly IStatsService _statsService;

    public TestController(IStatsService statsService)
    {
        _statsService = statsService;
    }

    [HttpGet]
    public ActionResult<object> Get()
    {
        return Ok(new { message = "API is working!", timestamp = DateTime.UtcNow });
    }

    [HttpGet("health")]
    public ActionResult<object> Health()
    {
        return Ok(new { status = "healthy", version = "1.0.0" });
    }

    [HttpGet("db-test")]
    public async Task<ActionResult<object>> DatabaseTest()
    {
        try
        {
            // Create a test UUID (simulating a Supabase Auth user ID)
            var testUserId = Guid.NewGuid().ToString();
            
            // Test creating and getting user stats
            var stats = await _statsService.GetUserStatsAsync(testUserId);
            
            return Ok(new 
            { 
                message = "Database connection successful!", 
                testUserId = testUserId,
                stats = stats 
            });
        }
        catch (Exception ex)
        {
            return BadRequest(new 
            { 
                error = "Database connection failed", 
                details = ex.Message,
                innerException = ex.InnerException?.Message
            });
        }
    }
} 