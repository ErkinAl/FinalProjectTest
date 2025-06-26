using Supabase.Postgrest.Attributes;
using Supabase.Postgrest.Models;

namespace MuvTimeAPI.Models;

[Table("exercise_sessions")]
public class ExerciseSession : BaseModel
{
    [PrimaryKey("id")]
    public string Id { get; set; } = string.Empty;

    [Column("user_id")]
    public string UserId { get; set; } = string.Empty;

    [Column("exercise_type")]
    public string ExerciseType { get; set; } = "jump_counter";

    [Column("jumps_completed")]
    public int JumpsCompleted { get; set; } = 0;

    [Column("xp_earned")]
    public int XpEarned { get; set; } = 0;

    [Column("session_duration")]
    public int SessionDuration { get; set; } = 0;

    [Column("completed_at")]
    public DateTime CompletedAt { get; set; }
} 