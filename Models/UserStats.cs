using Supabase.Postgrest.Attributes;
using Supabase.Postgrest.Models;

namespace MuvTimeAPI.Models;

[Table("user_stats")]
public class UserStats : BaseModel
{
    [PrimaryKey("id")]
    public string Id { get; set; } = string.Empty;

    [Column("user_id")]
    public string UserId { get; set; } = string.Empty;

    [Column("xp")]
    public int Xp { get; set; } = 0;

    [Column("level")]
    public int Level { get; set; } = 0;

    [Column("total_jumps")]
    public int TotalJumps { get; set; } = 0;

    [Column("exercises_completed")]
    public int ExercisesCompleted { get; set; } = 0;

    [Column("created_at")]
    public DateTime CreatedAt { get; set; }

    [Column("updated_at")]
    public DateTime UpdatedAt { get; set; }
} 