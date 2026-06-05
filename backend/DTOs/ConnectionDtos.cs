namespace Moment.Api.DTOs;

using Moment.Api.Models;

public record ConnectionDto(
    Guid Id,
    UserDto OtherUser,
    ConnectionStatus Status,
    bool IsRequester,
    DateTime CreatedAt
);

public record InviteDto(
    string InviteCode,
    string InviteUrl,
    DateTime ExpiresAt
);

public record ConnectionRequest(Guid TargetUserId);

public record RespondToConnectionRequest(Guid ConnectionId, bool Accept);
