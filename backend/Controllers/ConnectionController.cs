using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Moment.Api.DTOs;
using Moment.Api.Services;
using System.Security.Claims;

namespace Moment.Api.Controllers;

[Authorize]
[ApiController]
[Route("api/v1/connections")]
public class ConnectionController : ControllerBase
{
    private readonly IConnectionService _connectionService;

    public ConnectionController(IConnectionService connectionService)
    {
        _connectionService = connectionService;
    }

    [HttpPost("invite")]
    public async Task<IActionResult> CreateInvite()
    {
        var userId = GetUserId();
        var invite = await _connectionService.CreateInviteAsync(userId);
        return Ok(invite);
    }

    [AllowAnonymous]
    [HttpGet("invite/{inviteCode}")]
    public async Task<IActionResult> GetInviteInfo(string inviteCode)
    {
        var user = await _connectionService.GetUserByInviteCodeAsync(inviteCode);
        if (user == null) return NotFound("Invalid or expired invite code");

        var userAgent = Request.Headers["User-Agent"].ToString();
        if (!Request.Headers.ContainsKey("X-Requested-With") && 
            (userAgent.Contains("Mozilla") || userAgent.Contains("Chrome") || userAgent.Contains("Safari")))
        {
            var playStoreUrl = $"https://play.google.com/store/apps/details?id=com.moment.app&referrer=moment_invite_code%3D{inviteCode}";
            return Redirect(playStoreUrl);
        }

        return Ok(user);
    }

    [HttpPost("request")]
    public async Task<IActionResult> RequestConnection([FromBody] CreateConnectionRequest request)
    {
        var userId = GetUserId();
        var result = await _connectionService.RequestConnectionAsync(userId, request.TargetUserId);
        if (result == null) return BadRequest("Could not request connection");
        return Ok(result);
    }

    [HttpPost("respond")]
    public async Task<IActionResult> RespondToRequest([FromBody] RespondToConnectionRequest request)
    {
        var userId = GetUserId();
        var success = await _connectionService.RespondToRequestAsync(userId, request.RequestId, request.Accept);
        if (!success) return BadRequest("Could not respond to request");
        return Ok(new { success = true });
    }

    [HttpGet]
    public async Task<IActionResult> GetConnections()
    {
        var userId = GetUserId();
        var result = await _connectionService.GetConnectionsAsync(userId);
        return Ok(result);
    }

    [HttpGet("requests/pending")]
    public async Task<IActionResult> GetPendingRequests()
    {
        var userId = GetUserId();
        var result = await _connectionService.GetPendingRequestsAsync(userId);
        return Ok(result);
    }

    [HttpGet("requests/sent")]
    public async Task<IActionResult> GetSentRequests()
    {
        var userId = GetUserId();
        var result = await _connectionService.GetSentRequestsAsync(userId);
        return Ok(result);
    }

    [HttpDelete("{targetUserId}")]
    public async Task<IActionResult> RevokeConnection(Guid targetUserId)
    {
        var userId = GetUserId();
        var success = await _connectionService.RevokeConnectionAsync(userId, targetUserId);
        if (!success) return BadRequest("Could not revoke connection");
        return Ok(new { success = true });
    }

    private Guid GetUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier);
        return Guid.Parse(userIdClaim!.Value);
    }
}
