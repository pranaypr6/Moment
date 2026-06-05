using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Moment.Api.Models;

public class Invite
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public string InviteCode { get; set; } = string.Empty;

    [Required]
    public Guid SenderUserId { get; set; }
    
    [ForeignKey("SenderUserId")]
    public User? Sender { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    
    public DateTime ExpiresAt { get; set; } = DateTime.UtcNow.AddDays(7);
    
    public bool IsUsed { get; set; } = false;
}
