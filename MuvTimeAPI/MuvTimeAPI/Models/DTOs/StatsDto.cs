namespace MuvTimeAPI.Models.DTOs;

public class StatsDto
{
    public int Level { get; set; }
    public int Xp { get; set; }
    public int TotalJumps { get; set; }
    public int TotalArmCircles { get; set; }
    public int TotalHighKnees { get; set; }
    public int TotalSideReaches { get; set; }
    public int TotalJackJumps { get; set; }
    public int TotalBicepsCurls { get; set; }
    public int TotalShoulderPresses { get; set; }
    public int TotalSquats { get; set; }
    public int ExercisesCompleted { get; set; }
    public int TotalAllExercises { get; set; }
    public int XpToNextLevel { get; set; }
    public int CurrentLevelXp { get; set; }
}

public class UpdateStatsRequest
{
    public string ExerciseType { get; set; } = "jump";
    public int RepsCompleted { get; set; }
    public int XpEarned { get; set; }
    public int SessionDuration { get; set; }
}

public class SessionDto
{
    public string Id { get; set; } = string.Empty;
    public string ExerciseType { get; set; } = string.Empty;
    public int RepsCompleted { get; set; }
    public int XpEarned { get; set; }
    public int SessionDuration { get; set; }
    public DateTime CompletedAt { get; set; }
} 