using System.ComponentModel.DataAnnotations;

namespace Moment.Api.Models;

public class Report
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid ReporterUserId { get; set; }
    
    public User? Reporter { get; set; }

    public Guid? ReportedUserId { get; set; }
    
    public User? ReportedUser { get; set; }

    public Guid? MomentId { get; set; }
    
    public WallpaperMoment? Moment { get; set; }

    [Required]
    public string Reason { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
