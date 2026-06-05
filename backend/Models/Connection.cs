using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Moment.Api.Models;

public enum ConnectionStatus
{
    PENDING,
    ACCEPTED,
    BLOCKED,
    REVOKED
}

public class Connection
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
    public ConnectionStatus Status { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}
