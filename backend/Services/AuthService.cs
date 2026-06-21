using Google.Apis.Auth;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Moment.Api.Data;
using Moment.Api.DTOs;
using Moment.Api.Models;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;

namespace Moment.Api.Services;

public interface IAuthService
{
    Task<AuthResponse?> LoginWithGoogleAsync(string idToken);
    Task<AuthUserDto?> GetProfileAsync(Guid userId);
    Task<AuthUserDto?> UpdateProfileAsync(Guid userId, string displayName, string? profilePictureUrl);
    Task<AuthUserDto?> CreateProfileAsync(Guid userId, CreateProfileRequest request);
    Task<bool> IsUsernameAvailableAsync(string username);
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
            var payload = await GoogleJsonWebSignature.ValidateAsync(idToken);
            
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
            return new AuthResponse(token, MapToDto(user));
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

        await _context.SaveChangesAsync();
        return MapToDto(user);
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
            Expires = DateTime.UtcNow.AddDays(30),
            Issuer = _configuration["Jwt:Issuer"],
            Audience = _configuration["Jwt:Audience"],
            SigningCredentials = new SigningCredentials(new SymmetricSecurityKey(key), SecurityAlgorithms.HmacSha256Signature)
        };
        var token = tokenHandler.CreateToken(tokenDescriptor);
        return tokenHandler.WriteToken(token);
    }

    private AuthUserDto MapToDto(User user) => new AuthUserDto(
        user.Id,
        user.Email,
        user.Username,
        user.DisplayName,
        user.ProfilePictureUrl
    );
}
