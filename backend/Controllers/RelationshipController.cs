using System;
using System.Security.Claims;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Moment.Api.DTOs;
using Moment.Api.Services;

namespace Moment.Api.Controllers;

[Authorize]
[ApiController]
[Route("api/[controller]")]
public class RelationshipController : ControllerBase
{
    private readonly IRelationshipService _relationshipService;

    public RelationshipController(IRelationshipService relationshipService)
    {
        _relationshipService = relationshipService;
    }

    private Guid GetUserId() => Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

    [HttpGet("current")]
    public async Task<IActionResult> GetCurrentRelationship()
    {
        var rel = await _relationshipService.GetCurrentRelationshipAsync(GetUserId());
        if (rel == null) return NotFound();
        return Ok(rel);
    }

    [HttpPost("pairing-key")]
    public async Task<IActionResult> CreatePairingKey()
    {
        var key = await _relationshipService.CreatePairingKeyAsync(GetUserId());
        return Ok(key);
    }

    [HttpPost("join")]
    [Microsoft.AspNetCore.RateLimiting.EnableRateLimiting("JoinLimiter")]
    public async Task<IActionResult> Join([FromBody] JoinRelationshipRequest req)
    {
        try
        {
            var rel = await _relationshipService.JoinRelationshipAsync(GetUserId(), req.PairingKey);
            return Ok(rel);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error joining relationship: {ex}");
            return StatusCode(500, "An internal error occurred.");
        }
    }

    [HttpPut("space-name")]
    public async Task<IActionResult> UpdateSpaceName([FromBody] UpdateSpaceNameRequest req)
    {
        try
        {
            var rel = await _relationshipService.UpdateSpaceNameAsync(GetUserId(), req.SpaceName);
            return Ok(rel);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }

    [HttpPut("theme")]
    public async Task<IActionResult> UpdateTheme([FromBody] UpdateThemeRequest req)
    {
        try
        {
            var rel = await _relationshipService.UpdateThemeAsync(GetUserId(), req.ThemeId);
            return Ok(rel);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }

    [HttpPut("cover")]
    public async Task<IActionResult> UpdateCover([FromBody] UpdateCoverRequest req)
    {
        try
        {
            var rel = await _relationshipService.UpdateCoverAsync(GetUserId(), req.CoverMomentId);
            return Ok(rel);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }

    [HttpPut("pause")]
    public async Task<IActionResult> SetPause([FromBody] PauseRequest req)
    {
        try
        {
            var rel = await _relationshipService.SetPauseAsync(GetUserId(), req.IsPaused);
            return Ok(rel);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }

    [HttpPost("unpair")]
    public async Task<IActionResult> Unpair()
    {
        await _relationshipService.UnpairAsync(GetUserId());
        return Ok();
    }
}
