using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Moment.Api.Models;

public enum WallpaperTarget
{
    HOME,
    LOCK,
    BOTH
}

public enum MomentStatus
{
    PENDING,
    DELIVERED,
    APPLIED,
    FAILED
}

public class WallpaperMoment
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid SenderUserId { get; set; }
    
    [ForeignKey("SenderUserId")]
    public User? Sender { get; set; }

    [Required]
    public Guid ReceiverUserId { get; set; }
    
    [ForeignKey("ReceiverUserId")]
    public User? Receiver { get; set; }

    [Required]
    public string ImageUrl { get; set; } = string.Empty;

    public string? ThumbnailUrl { get; set; }

    public string? Note { get; set; }

    [Required]
    public WallpaperTarget WallpaperTarget { get; set; }

    [Required]
    public MomentStatus Status { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    
    public DateTime? DeliveredAt { get; set; }
    
    public DateTime? AppliedAt { get; set; }
}
