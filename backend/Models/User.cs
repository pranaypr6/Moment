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
    
    public bool IsPremium { get; set; } = false;
    
    public DateTime? TermsAcceptedAt { get; set; }
    
    public DateTime? PrivacyAcceptedAt { get; set; }
    
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public string? RefreshToken { get; set; }
    
    public DateTime? RefreshTokenExpiryTime { get; set; }
}
