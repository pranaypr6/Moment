namespace Moment.Api.DTOs;

public record GoogleLoginRequest(string IdToken);

public record AuthResponse(string Token, UserDto User);

public record UserDto(
    Guid Id,
    string Email,
    string? Username,
    string? DisplayName,
    string? ProfilePictureUrl,
    string? Bio
);

public record CreateProfileRequest(
    string Username,
    string DisplayName,
    string? Bio,
    string? ProfilePictureUrl
);

public record UpdateProfileRequest(
    string DisplayName,
    string? ProfilePictureUrl
);
