namespace Moment.Api.DTOs;

public record UserDto(
    Guid Id,
    string DisplayName,
    string? ProfilePictureUrl
);

public record RelationshipDto(
    Guid Id,
    UserDto Partner,
    string SpaceName,
    string ThemeId,
    Guid? CoverMomentId,
    bool IsPausedByMe,
    bool IsPausedByPartner,
    string Status,
    DateTime CreatedAt,
    DateTime? PairedAt,
    int TotalMoments = 0,
    Dictionary<string, int>? SignalsCount = null
);

public record CreatePairingKeyResponse(string PairingKey, DateTime ExpiresAt);

public record JoinRelationshipRequest(string PairingKey);

public record UpdateSpaceNameRequest(string SpaceName);

public record UpdateThemeRequest(string ThemeId);

public record UpdateCoverRequest(Guid CoverMomentId);
