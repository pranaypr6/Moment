using System.ComponentModel.DataAnnotations;

namespace Moment.Api.Models;

public class User
{
    [Key]
    public Guid Id { get; set; }
    
    [Required]
    public string FirebaseUid { get; set; } = string.Empty;
    
    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;
    
    public string? Username { get; set; }
    
    public string? DisplayName { get; set; }
    
    public string? ProfilePictureUrl { get; set; }
    
    public string? CurrentVibe { get; set; }
    
    public DateTime? VibeUpdatedAt { get; set; }
    

    public DateTime? TermsAcceptedAt { get; set; }
    
    public DateTime? PrivacyAcceptedAt { get; set; }
    
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public string? RefreshToken { get; set; }

    public DateTime? RefreshTokenExpiryTime { get; set; }

    // Holds the immediately-preceding refresh token (and its own expiry) for a short grace
    // window after rotation. Mobile clients on flaky networks can send a refresh request,
    // have the server rotate the token successfully, and then never see the response (timeout/
    // dropped connection). The client's only copy of the token is now stale, so its next retry
    // would otherwise be rejected as "invalid", forcing a real logout even though nothing was
    // actually wrong with the session. Accepting the previous token for a brief window lets that
    // retry succeed instead.
    public string? PreviousRefreshToken { get; set; }

    public DateTime? PreviousRefreshTokenExpiryTime { get; set; }
}
