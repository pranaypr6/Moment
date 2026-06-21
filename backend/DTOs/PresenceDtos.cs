using System;
using Moment.Api.Models;

namespace Moment.Api.DTOs;

public class SendPresenceRequest
{
    public Guid RelationshipId { get; set; }
    public PresenceSignalType Type { get; set; }
}

public class PresenceSignalDto
{
    public Guid Id { get; set; }
    public Guid RelationshipId { get; set; }
    public Guid SenderUserId { get; set; }
    public Guid ReceiverUserId { get; set; }
    public PresenceSignalType Type { get; set; }
    public DateTime CreatedAtUtc { get; set; }
}
