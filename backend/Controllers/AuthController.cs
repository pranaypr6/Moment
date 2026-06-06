using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Moment.Api.DTOs;
using Moment.Api.Services;
using System.Security.Claims;

namespace Moment.Api.Controllers;

[ApiController]
[Route("api/v1/auth")]
public class AuthController : ControllerBase
{
    private readonly IAuthService _authService;

    public AuthController(IAuthService authService)
    {
        _authService = authService;
    }

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
        var user = await _authService.UpdateProfileAsync(userId, request.DisplayName, request.ProfilePictureUrl);
        if (user == null) return NotFound();

        return Ok(user);
    }

    [Authorize]
    [HttpPost("profile")]
    public async Task<IActionResult> CreateProfile([FromBody] CreateProfileRequest request)
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier);
        if (userIdClaim == null) return Unauthorized();

        var userId = Guid.Parse(userIdClaim.Value);
        var result = await _authService.CreateProfileAsync(userId, request);

        if (result == null) return BadRequest("Username already taken or user not found");
        return Ok(result);
    }

    [HttpGet("username-available")]
    public async Task<IActionResult> IsUsernameAvailable([FromQuery] string username)
    {
        if (string.IsNullOrWhiteSpace(username)) return BadRequest("Username is required");
        var available = await _authService.IsUsernameAvailableAsync(username);
        return Ok(new { available });
    }
}
