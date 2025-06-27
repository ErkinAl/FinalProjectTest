namespace MuvTimeAPI.Models.DTOs;

public class ExerciseSessionDto
{
    public string Id { get; set; } = string.Empty;
    public string ExerciseType { get; set; } = string.Empty;
    public int JumpsCompleted { get; set; }
    public int XpEarned { get; set; }
    public int SessionDuration { get; set; }
    public DateTime CompletedAt { get; set; }
} 