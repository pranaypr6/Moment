using System.Text.Json.Serialization;

namespace Moment.Api.DTOs;

public class GoogleLoginRequest
{
    [JsonPropertyName("idToken")]
    public string IdToken { get; set; } = string.Empty;
}

public record AuthResponse(string Token, AuthUserDto User);

public record AuthUserDto(
    Guid Id,
    string Email,
    string? Username,
    string? DisplayName,
    string? ProfilePictureUrl,
    string? CurrentVibe,
    bool IsPremium
);

public record CreateProfileRequest(
    string Username,
    string DisplayName,
    string? ProfilePictureUrl
);

public record UpdateProfileRequest(
    string DisplayName,
    string? ProfilePictureUrl
);

public record UpdateVibeRequest(
    string Vibe
);
