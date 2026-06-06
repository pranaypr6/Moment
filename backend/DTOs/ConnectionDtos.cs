namespace Moment.Api.DTOs;

using Moment.Api.Models;

public record ConnectionDto(
    Guid TargetUserId,
    UserDto OtherUser,
    string? Alias,
    bool IsMuted,
    bool IsPinned,
    DateTime ConnectedAt
);

public record ConnectionRequestDto(
    Guid Id,
    UserDto OtherUser,
    RequestStatus Status,
    DateTime CreatedAt
);

public record InviteDto(
    string InviteCode,
    string InviteUrl,
    DateTime ExpiresAt
);

public record CreateConnectionRequest(Guid TargetUserId);

public record RespondToConnectionRequest(Guid RequestId, bool Accept);
