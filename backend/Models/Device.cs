using System.ComponentModel.DataAnnotations;

namespace Moment.Api.Models;

public class Device
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid UserId { get; set; }
    
    public User? User { get; set; }

    [Required]
    public string FcmToken { get; set; } = string.Empty;

    public string? Platform { get; set; }
    
    public string? DeviceName { get; set; }

    public DateTime LastSeenAt { get; set; } = DateTime.UtcNow;
    
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
