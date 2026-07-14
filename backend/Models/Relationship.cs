using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Moment.Api.Models;

public enum RelationshipStatus { Pending, Active, Unpaired }

public class Relationship
{
    [Key]
    public Guid Id { get; set; }
    
    [Required]
    public Guid Partner1Id { get; set; }
    [ForeignKey("Partner1Id")] public User? Partner1 { get; set; }

    [Required]
    public Guid Partner2Id { get; set; }
    [ForeignKey("Partner2Id")] public User? Partner2 { get; set; }

    [Required]
    public Guid CreatedByUserId { get; set; }

    public string SpaceName { get; set; } = "❤️ Us";
    public string ThemeId { get; set; } = "default";
    
    public DateTime? AnniversaryDate { get; set; }
    
    public Guid? CoverMomentId { get; set; }
    [ForeignKey("CoverMomentId")] public WallpaperMoment? CoverMoment { get; set; }

    public DateTime? Partner1PausedAt { get; set; }
    public DateTime? Partner2PausedAt { get; set; }

    [Required]
    public RelationshipStatus Status { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? PairedAt { get; set; }
    public DateTime? UnpairedAt { get; set; }
}
