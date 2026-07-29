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

    Task<bool> DeleteAccountAsync(Guid userId);
    Task<AuthResponse?> RefreshTokenAsync(string refreshToken);
    Task RevokeSessionAsync(Guid userId);
}

public class AuthService : IAuthService
{
    private readonly MomentDbContext _context;
    private readonly IConfiguration _configuration;
    private readonly IPushNotificationService _pushNotificationService;
    private readonly IStorageService _storageService;
    private readonly Microsoft.Extensions.Logging.ILogger<AuthService> _logger;

    public AuthService(
        MomentDbContext context,
        IConfiguration configuration,
        IPushNotificationService pushNotificationService,
        IStorageService storageService,
        Microsoft.Extensions.Logging.ILogger<AuthService> logger)
    {
        _context = context;
        _configuration = configuration;
        _pushNotificationService = pushNotificationService;
        _storageService = storageService;
        _logger = logger;
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
            _logger.LogWarning(ex, "Google token validation failed.");
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
            if (!IsAllowedProfilePictureUrl(profilePictureUrl))
            {
                throw new InvalidOperationException("Profile picture URL must point to the app's own storage or a Google account photo.");
            }
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

        if (!string.IsNullOrEmpty(request.ProfilePictureUrl) && !IsAllowedProfilePictureUrl(request.ProfilePictureUrl))
        {
            throw new InvalidOperationException("Profile picture URL must point to the app's own storage or a Google account photo.");
        }

        // TermsAcceptedAt/PrivacyAcceptedAt existed on the schema but nothing ever wrote to
        // them - there was no actual consent capture anywhere in the signup flow. Profile
        // creation is the one point every user passes through exactly once, so it's the
        // right place to require and record it.
        if (!request.AcceptedTerms)
        {
            throw new InvalidOperationException("You must accept the Terms of Service and Privacy Policy to continue.");
        }

        user.Username = request.Username.ToLower().Trim();
        user.DisplayName = request.DisplayName;
        user.ProfilePictureUrl = request.ProfilePictureUrl ?? user.ProfilePictureUrl;
        user.TermsAcceptedAt = DateTime.UtcNow;
        user.PrivacyAcceptedAt = DateTime.UtcNow;
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

        if (string.IsNullOrWhiteSpace(vibe))
        {
            user.CurrentVibe = null;
            user.VibeUpdatedAt = null;
        }
        else
        {
            user.CurrentVibe = vibe;
            user.VibeUpdatedAt = DateTime.UtcNow;
            
            // Notify partner
            var activeRelationship = await _context.Relationships
                .Include(r => r.Partner1)
                .Include(r => r.Partner2)
                .FirstOrDefaultAsync(r => (r.Partner1Id == userId || r.Partner2Id == userId) && r.Status == RelationshipStatus.Active);
                
            if (activeRelationship != null)
            {
                var partnerId = activeRelationship.Partner1Id == userId ? activeRelationship.Partner2Id : activeRelationship.Partner1Id;
                var senderName = user.DisplayName ?? "Your partner";
                await _pushNotificationService.SendVibeUpdateNotificationAsync(partnerId, senderName, vibe);
            }
        }

        user.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return MapToDto(user);
    }



    public async Task<bool> DeleteAccountAsync(Guid userId)
    {
        var user = await _context.Users.FindAsync(userId);
        if (user == null) return false;

        // Gather everything that references this user BEFORE deleting anything,
        // so we can also clean up R2 media after the DB transaction commits.
        var relationships = await _context.Relationships
            .Where(r => r.Partner1Id == userId || r.Partner2Id == userId)
            .ToListAsync();
        var relationshipIds = relationships.Select(r => r.Id).ToList();

        var moments = await _context.Moments
            .Where(m => relationshipIds.Contains(m.RelationshipId)
                || m.CreatorUserId == userId
                || m.ReceiverUserId == userId)
            .ToListAsync();

        var devices = await _context.Devices.Where(d => d.UserId == userId).ToListAsync();
        var invites = await _context.Invites.Where(i => i.SenderUserId == userId).ToListAsync();
        var reports = await _context.Reports
            .Where(r => r.ReporterUserId == userId || r.ReportedUserId == userId)
            .ToListAsync();
        var presenceSignals = await _context.PresenceSignals
            .Where(p => p.SenderUserId == userId || p.ReceiverUserId == userId)
            .ToListAsync();

        // Clear any relationship's CoverMomentId pointing at a moment we're about to delete,
        // to satisfy the SetNull FK before the moment rows are removed.
        foreach (var rel in relationships)
        {
            if (rel.CoverMomentId.HasValue && moments.Any(m => m.Id == rel.CoverMomentId))
            {
                rel.CoverMomentId = null;
            }
        }

        await using var transaction = await _context.Database.BeginTransactionAsync();
        try
        {
            _context.PresenceSignals.RemoveRange(presenceSignals);
            _context.Reports.RemoveRange(reports);
            _context.Moments.RemoveRange(moments);
            _context.Invites.RemoveRange(invites);
            _context.Devices.RemoveRange(devices);
            _context.Relationships.RemoveRange(relationships);
            _context.Users.Remove(user);

            await _context.SaveChangesAsync();
            await transaction.CommitAsync();
        }
        catch (DbUpdateException ex)
        {
            await transaction.RollbackAsync();
            _logger.LogError(ex, "Account deletion failed for user {UserId}; no data was deleted.", userId);
            throw;
        }

        // DB deletion is the source of truth for "account deleted" and has already
        // committed. R2 media cleanup is best-effort from here: log failures instead
        // of failing the whole request, since the user's data is already gone from the DB.
        foreach (var moment in moments)
        {
            foreach (var url in new[] { moment.ImageUrl, moment.ThumbnailUrl })
            {
                if (string.IsNullOrWhiteSpace(url)) continue;
                try
                {
                    var fileName = new Uri(url).Segments.Last();
                    await _storageService.DeleteFileAsync(fileName);
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to delete R2 object for moment {MomentId} during account deletion of user {UserId}.", moment.Id, userId);
                }
            }
        }

        if (!string.IsNullOrWhiteSpace(user.ProfilePictureUrl))
        {
            try
            {
                var fileName = new Uri(user.ProfilePictureUrl).Segments.Last();
                await _storageService.DeleteFileAsync(fileName);
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Failed to delete profile picture R2 object during account deletion of user {UserId}.", userId);
            }
        }

        return true;
    }

    public async Task<AuthResponse?> RefreshTokenAsync(string refreshToken)
    {
        var hashedToken = HashRefreshToken(refreshToken);
        var now = DateTime.UtcNow;

        // Match against the current token OR the immediately-preceding one, as long as the
        // latter is still inside its short grace window. Without the second branch, a client
        // whose rotation response got lost in transit (flaky mobile network) would retry with
        // a token the server already rotated away from, get correctly-but-unhelpfully rejected
        // as "invalid", and be forced into a real logout even though the session itself was
        // never actually compromised. See PreviousRefreshToken on User for more detail.
        var user = await _context.Users.FirstOrDefaultAsync(u =>
            (u.RefreshToken == hashedToken && u.RefreshTokenExpiryTime > now) ||
            (u.PreviousRefreshToken == hashedToken && u.PreviousRefreshTokenExpiryTime > now));

        if (user == null)
        {
            return null; // Invalid or expired refresh token (current and grace-window copy both missed)
        }

        var newJwtToken = GenerateJwtToken(user);
        var newRefreshToken = GenerateRefreshToken();

        // Whatever was current (whether or not this request matched via it) becomes the grace-
        // window fallback for the next couple of minutes; the freshly generated token becomes
        // the new current.
        user.PreviousRefreshToken = user.RefreshToken;
        user.PreviousRefreshTokenExpiryTime = now.AddMinutes(2);
        user.RefreshToken = HashRefreshToken(newRefreshToken);
        user.RefreshTokenExpiryTime = now.AddDays(30);

        await _context.SaveChangesAsync();

        return new AuthResponse(newJwtToken, newRefreshToken, MapToDto(user));
    }

    // There was previously no way to kill a refresh token server-side - a lost/stolen
    // device or a "log out" tap only cleared local prefs on that one device, and the
    // refresh token sitting server-side stayed valid for its full 30-day lifetime
    // (plus the 2-minute PreviousRefreshToken grace window). Clearing both here means
    // logging out actually revokes the session instead of just hiding it locally; the
    // still-valid short-lived JWT already in flight will still work until it naturally
    // expires, but no further refresh will succeed.
    public async Task RevokeSessionAsync(Guid userId)
    {
        var user = await _context.Users.FindAsync(userId);
        if (user == null) return;

        user.RefreshToken = null;
        user.RefreshTokenExpiryTime = null;
        user.PreviousRefreshToken = null;
        user.PreviousRefreshTokenExpiryTime = null;

        await _context.SaveChangesAsync();
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

    // MomentService already rejects moment image/thumbnail URLs that don't point at our
    // own R2 storage domain, but these two profile endpoints accepted whatever URL string
    // the client sent, unvalidated. Since the profile picture is rendered as a trusted
    // image for the *other* partner (unpair/report/hub screens, notifications), a modified
    // client could point it at an attacker-controlled host - the same class of issue the
    // moment image check already guards against. The only two legitimate sources are our
    // own storage (client uploads a photo via the presigned-URL flow) or the Google account
    // photo returned at sign-in (googleusercontent.com), so only those are allowed.
    private bool IsAllowedProfilePictureUrl(string url)
    {
        var publicUrlPrefix = (_configuration["Cloudflare:PublicUrl"] ?? "").TrimEnd('/');
        if (!string.IsNullOrEmpty(publicUrlPrefix) && url.StartsWith(publicUrlPrefix, StringComparison.OrdinalIgnoreCase))
        {
            return true;
        }

        if (Uri.TryCreate(url, UriKind.Absolute, out var uri) &&
            uri.Scheme == Uri.UriSchemeHttps &&
            uri.Host.EndsWith("googleusercontent.com", StringComparison.OrdinalIgnoreCase))
        {
            return true;
        }

        return false;
    }

    private AuthUserDto MapToDto(User user)
    {
        var activeVibe = user.VibeUpdatedAt.HasValue && (DateTime.UtcNow - user.VibeUpdatedAt.Value).TotalHours < 24 
            ? user.CurrentVibe 
            : null;

        return new AuthUserDto(
            user.Id,
            user.Email,
            user.Username,
            user.DisplayName,
            user.ProfilePictureUrl,
            activeVibe
        );
    }
}
