using Google.Apis.Auth;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Moment.Api.Data;
using Moment.Api.DTOs;
using Moment.Api.Models;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;

namespace Moment.Api.Services;

public interface IAuthService
{
    Task<AuthResponse?> LoginWithGoogleAsync(string idToken);
    Task<AuthUserDto?> GetProfileAsync(Guid userId);
    Task<AuthUserDto?> UpdateProfileAsync(Guid userId, string displayName, string? profilePictureUrl);
    Task<AuthUserDto?> CreateProfileAsync(Guid userId, CreateProfileRequest request);
    Task<bool> IsUsernameAvailableAsync(string username);
    Task<AuthUserDto?> UpdateVibeAsync(Guid userId, string vibe);
    Task<AuthUserDto?> UpgradeToPremiumAsync(Guid userId);
    Task DeleteAccountAsync(Guid userId);
    Task<AuthResponse?> RefreshTokenAsync(string refreshToken);
}

public class AuthService : IAuthService
{
    private readonly MomentDbContext _context;
    private readonly IConfiguration _configuration;

    public AuthService(MomentDbContext context, IConfiguration configuration)
    {
        _context = context;
        _configuration = configuration;
    }

    public async Task<AuthResponse?> LoginWithGoogleAsync(string idToken)
    {
        try
        {
            var clientId = _configuration["GoogleClientId"];
            if (string.IsNullOrEmpty(clientId))
            {
                throw new InvalidOperationException("GoogleClientId is missing from configuration.");
            }

            var settings = new GoogleJsonWebSignature.ValidationSettings
            {
                Audience = new[] { clientId }
            };

            var payload = await GoogleJsonWebSignature.ValidateAsync(idToken, settings);
            
            var uid = payload.Subject;
            var email = payload.Email ?? "";
            var name = payload.Name ?? "";
            var picture = payload.Picture ?? "";

            var user = await _context.Users.FirstOrDefaultAsync(u => u.FirebaseUid == uid);

            if (user == null)
            {
                user = new User
                {
                    Id = Guid.NewGuid(),
                    FirebaseUid = uid,
                    Email = email,
                    DisplayName = name,
                    ProfilePictureUrl = picture,
                    CreatedAt = DateTime.UtcNow,
                    UpdatedAt = DateTime.UtcNow
                };
                _context.Users.Add(user);
                await _context.SaveChangesAsync();
            }

            var token = GenerateJwtToken(user);
            var refreshToken = GenerateRefreshToken();
            
            user.RefreshToken = HashRefreshToken(refreshToken);
            user.RefreshTokenExpiryTime = DateTime.UtcNow.AddDays(30);
            await _context.SaveChangesAsync();

            return new AuthResponse(token, refreshToken, MapToDto(user));
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Token validation failed: {ex.Message}");
            return null;
        }
    }

    public async Task<AuthUserDto?> GetProfileAsync(Guid userId)
    {
        var user = await _context.Users.FindAsync(userId);
        if (user == null) return null;
        return MapToDto(user);
    }

    public async Task<AuthUserDto?> UpdateProfileAsync(Guid userId, string displayName, string? profilePictureUrl)
    {
        var user = await _context.Users.FindAsync(userId);
        if (user == null) return null;

        user.DisplayName = displayName;
        if (profilePictureUrl != null)
        {
            user.ProfilePictureUrl = profilePictureUrl;
        }
        user.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return MapToDto(user);
    }

    public async Task<AuthUserDto?> CreateProfileAsync(Guid userId, CreateProfileRequest request)
    {
        var user = await _context.Users.FindAsync(userId);
        if (user == null) return null;

        if (!await IsUsernameAvailableAsync(request.Username))
            return null;

        user.Username = request.Username.ToLower().Trim();
        user.DisplayName = request.DisplayName;
        user.ProfilePictureUrl = request.ProfilePictureUrl ?? user.ProfilePictureUrl;
        user.UpdatedAt = DateTime.UtcNow;

        try
        {
            await _context.SaveChangesAsync();
        }
        catch (Microsoft.EntityFrameworkCore.DbUpdateException)
        {
            return null; // Username claimed by concurrent request
        }
        
        return MapToDto(user);
    }

    public async Task<AuthUserDto?> UpdateVibeAsync(Guid userId, string vibe)
    {
        var user = await _context.Users.FindAsync(userId);
        if (user == null) return null;

        user.CurrentVibe = vibe;
        user.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return MapToDto(user);
    }

    public async Task<AuthUserDto?> UpgradeToPremiumAsync(Guid userId)
    {
        var user = await _context.Users.FindAsync(userId);
        if (user == null) return null;

        user.IsPremium = true;
        user.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return MapToDto(user);
    }

    public async Task DeleteAccountAsync(Guid userId)
    {
        var user = await _context.Users.FindAsync(userId);
        if (user != null)
        {
            var rel = await _context.Relationships
                .FirstOrDefaultAsync(r => r.Partner1Id == userId || r.Partner2Id == userId);
            
            if (rel != null)
            {
                _context.Relationships.Remove(rel);
            }
            
            _context.Users.Remove(user);
            try
            {
                await _context.SaveChangesAsync();
            }
            catch (DbUpdateException)
            {
                // Handle or log the error as necessary
            }
        }
    }

    public async Task<AuthResponse?> RefreshTokenAsync(string refreshToken)
    {
        var hashedToken = HashRefreshToken(refreshToken);
        var user = await _context.Users.FirstOrDefaultAsync(u => u.RefreshToken == hashedToken);
        if (user == null || user.RefreshTokenExpiryTime <= DateTime.UtcNow)
        {
            return null; // Invalid or expired refresh token
        }

        var newJwtToken = GenerateJwtToken(user);
        var newRefreshToken = GenerateRefreshToken();

        user.RefreshToken = HashRefreshToken(newRefreshToken);
        user.RefreshTokenExpiryTime = DateTime.UtcNow.AddDays(30);
        
        await _context.SaveChangesAsync();

        return new AuthResponse(newJwtToken, newRefreshToken, MapToDto(user));
    }

    public async Task<bool> IsUsernameAvailableAsync(string username)
    {
        var normalizedUsername = username.ToLower().Trim();
        return !await _context.Users.AnyAsync(u => u.Username == normalizedUsername);
    }

    private string GenerateJwtToken(User user)
    {
        var tokenHandler = new JwtSecurityTokenHandler();
        var key = Encoding.UTF8.GetBytes(_configuration["Jwt:Key"] ?? throw new InvalidOperationException("JWT Key is missing"));
        var tokenDescriptor = new SecurityTokenDescriptor
        {
            Subject = new ClaimsIdentity(new[]
            {
                new Claim(ClaimTypes.NameIdentifier, user.Id.ToString()),
                new Claim(ClaimTypes.Email, user.Email),
                new Claim("username", user.Username ?? "")
            }),
            Expires = DateTime.UtcNow.AddMinutes(15),
            Issuer = _configuration["Jwt:Issuer"],
            Audience = _configuration["Jwt:Audience"],
            SigningCredentials = new SigningCredentials(new SymmetricSecurityKey(key), SecurityAlgorithms.HmacSha256Signature)
        };
        var token = tokenHandler.CreateToken(tokenDescriptor);
        return tokenHandler.WriteToken(token);
    }

    private string GenerateRefreshToken()
    {
        var randomNumber = new byte[64];
        using var rng = RandomNumberGenerator.Create();
        rng.GetBytes(randomNumber);
        return Convert.ToBase64String(randomNumber);
    }

    private string HashRefreshToken(string refreshToken)
    {
        using var sha256 = SHA256.Create();
        var bytes = Encoding.UTF8.GetBytes(refreshToken);
        var hash = sha256.ComputeHash(bytes);
        return Convert.ToBase64String(hash);
    }

    private AuthUserDto MapToDto(User user) => new AuthUserDto(
        user.Id,
        user.Email,
        user.Username,
        user.DisplayName,
        user.ProfilePictureUrl,
        user.CurrentVibe,
        user.IsPremium
    );
}
