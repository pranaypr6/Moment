using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace Moment.Api.DTOs;

public class GoogleLoginRequest
{
    [JsonPropertyName("idToken")]
    public string IdToken { get; set; } = string.Empty;
}

public record AuthResponse(string Token, string RefreshToken, AuthUserDto User);

public record RefreshTokenRequest(string RefreshToken);

public record AuthUserDto(
    Guid Id,
    string Email,
    string? Username,
    string? DisplayName,
    string? ProfilePictureUrl,
    string? CurrentVibe
);

public record CreateProfileRequest(
    [Required]
    [RegularExpression("^[a-z0-9_]{4,20}$", ErrorMessage = "Username must be 4-20 characters: lowercase letters, numbers, and underscores only.")]
    string Username,
    [Required]
    [StringLength(50, MinimumLength = 1)]
    string DisplayName,
    [StringLength(2048)]
    string? ProfilePictureUrl,
    bool AcceptedTerms = false
);

public record UpdateProfileRequest(
    [Required]
    [StringLength(50, MinimumLength = 1)]
    string DisplayName,
    [StringLength(2048)]
    string? ProfilePictureUrl
);

public record UpdateVibeRequest(
    [StringLength(100)]
    string Vibe
);
