using System;

namespace Moment.Api.Models;

public enum PresenceSignalType
{
    ThinkingOfYou = 0,
    Punch = 1,
    Cuddle = 2,
    Kiss = 3,
    MissYou = 4
}

public class PresenceSignal
{
    public Guid Id { get; set; }
    
    public Guid RelationshipId { get; set; }
    public Relationship? Relationship { get; set; }
    
    public Guid SenderUserId { get; set; }
    public User? SenderUser { get; set; }
    
    public Guid ReceiverUserId { get; set; }
    public User? ReceiverUser { get; set; }
    
    public PresenceSignalType Type { get; set; }
    
    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;
}
