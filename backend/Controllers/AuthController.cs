using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Moment.Api.DTOs;
using Moment.Api.Services;
using System.Security.Claims;
using Microsoft.AspNetCore.RateLimiting;

namespace Moment.Api.Controllers;

[ApiController]
[Route("api/v1/auth")]
public class AuthController : ControllerBase
{
    private readonly IAuthService _authService;
    private readonly ILogger<AuthController> _logger;

    public AuthController(IAuthService authService, ILogger<AuthController> logger)
    {
        _authService = authService;
        _logger = logger;
    }

    [EnableRateLimiting("AuthLimiter")]
    [HttpPost("login/google")]
    public async Task<IActionResult> LoginWithGoogle([FromBody] GoogleLoginRequest request)
    {
        var result = await _authService.LoginWithGoogleAsync(request.IdToken);
        if (result == null) return Unauthorized("Invalid Google Token");
        return Ok(result);
    }

    [Authorize]
    [HttpGet("profile")]
    public async Task<IActionResult> GetProfile()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier);
        if (userIdClaim == null) return Unauthorized();

        var userId = Guid.Parse(userIdClaim.Value);
        var user = await _authService.GetProfileAsync(userId);
        if (user == null) return NotFound();

        return Ok(user);
    }

    [Authorize]
    [HttpPut("profile")]
    public async Task<IActionResult> UpdateProfile([FromBody] UpdateProfileRequest request)
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier);
        if (userIdClaim == null) return Unauthorized();

        var userId = Guid.Parse(userIdClaim.Value);
        try
        {
            var user = await _authService.UpdateProfileAsync(userId, request.DisplayName, request.ProfilePictureUrl);
            if (user == null) return NotFound();

            return Ok(user);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    [Authorize]
    [HttpDelete("me")]
    public async Task<IActionResult> DeleteAccount()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier);
        if (userIdClaim == null) return Unauthorized();

        var userId = Guid.Parse(userIdClaim.Value);

        try
        {
            var deleted = await _authService.DeleteAccountAsync(userId);
            if (!deleted) return NotFound(new { message = "Account not found." });
            return Ok(new { message = "Account and all associated data deleted." });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Account deletion failed for user {UserId}.", userId);
            return StatusCode(500, new { message = "Account deletion failed. Please try again or contact support." });
        }
    }

    [Authorize]
    [EnableRateLimiting("AuthLimiter")]
    [HttpPost("profile")]
    public async Task<IActionResult> CreateProfile([FromBody] CreateProfileRequest request)
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier);
        if (userIdClaim == null) return Unauthorized();

        var userId = Guid.Parse(userIdClaim.Value);
        try
        {
            var result = await _authService.CreateProfileAsync(userId, request);

            if (result == null) return BadRequest("Username already taken or user not found");
            return Ok(result);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    [HttpGet("username-available")]
    public async Task<IActionResult> IsUsernameAvailable([FromQuery] string username)
    {
        if (string.IsNullOrWhiteSpace(username)) return BadRequest("Username is required");
        var available = await _authService.IsUsernameAvailableAsync(username);
        return Ok(new { available });
    }

    [Authorize]
    [HttpPut("vibe")]
    public async Task<IActionResult> UpdateVibe([FromBody] UpdateVibeRequest request)
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier);
        if (userIdClaim == null) return Unauthorized();

        var userId = Guid.Parse(userIdClaim.Value);
        var user = await _authService.UpdateVibeAsync(userId, request.Vibe);
        if (user == null) return NotFound();

        return Ok(user);
    }


    [EnableRateLimiting("AuthLimiter")]
    [HttpPost("refresh")]
    public async Task<IActionResult> RefreshToken([FromBody] RefreshTokenRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.RefreshToken))
            return BadRequest("Refresh token is required.");

        var result = await _authService.RefreshTokenAsync(request.RefreshToken);
        if (result == null) return Unauthorized("Invalid or expired refresh token");

        return Ok(result);
    }
}
