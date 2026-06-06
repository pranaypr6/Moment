using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Moment.Api.Models;

public class UserConnection
{
    // Primary Key will be a composite of UserId and ConnectedUserId
    [Required]
    public Guid UserId { get; set; }
    
    [ForeignKey("UserId")]
    public User? User { get; set; }

    [Required]
    public Guid ConnectedUserId { get; set; }
    
    [ForeignKey("ConnectedUserId")]
    public User? ConnectedUser { get; set; }

    // This is where user-specific settings for the connection go
    public string? Alias { get; set; }
    public bool IsMuted { get; set; } = false;
    public bool IsPinned { get; set; } = false;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
