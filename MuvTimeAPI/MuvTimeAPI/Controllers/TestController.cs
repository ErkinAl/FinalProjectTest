using Microsoft.AspNetCore.Mvc;
using MuvTimeAPI.Services;

namespace MuvTimeAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class TestController : ControllerBase
{
    private readonly IStatsService _statsService;
    private readonly IConfiguration _configuration;

    public TestController(IStatsService statsService, IConfiguration configuration)
    {
        _statsService = statsService;
        _configuration = configuration;
    }

    [HttpGet]
    public ActionResult<object> Get()
    {
        return Ok(new { message = "API is working!", timestamp = DateTime.UtcNow });
    }

    [HttpGet("health")]
    public ActionResult<object> Health()
    {
        try
        {
            var supabaseUrl = _configuration["Supabase:Url"];
            var supabaseKey = _configuration["Supabase:Key"];
            var supabaseServiceKey = _configuration["Supabase:ServiceKey"];
            
            return Ok(new { 
                status = "healthy", 
                version = "1.0.0",
                environment = Environment.GetEnvironmentVariable("ASPNETCORE_ENVIRONMENT"),
                supabaseConfigured = new {
                    url = !string.IsNullOrEmpty(supabaseUrl) ? "configured" : "missing",
                    key = !string.IsNullOrEmpty(supabaseKey) ? "configured" : "missing",
                    serviceKey = !string.IsNullOrEmpty(supabaseServiceKey) ? "configured" : "missing"
                }
            });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { 
                status = "error", 
                message = ex.Message,
                innerException = ex.InnerException?.Message
            });
        }
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