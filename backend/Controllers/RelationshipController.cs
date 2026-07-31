using System;
using System.Security.Claims;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using Moment.Api.DTOs;
using Moment.Api.Services;

namespace Moment.Api.Controllers;

[Authorize]
[ApiController]
[Route("api/[controller]")]
public class RelationshipController : ControllerBase
{
    private readonly IRelationshipService _relationshipService;
    private readonly ILogger<RelationshipController> _logger;

    public RelationshipController(IRelationshipService relationshipService, ILogger<RelationshipController> logger)
    {
        _relationshipService = relationshipService;
        _logger = logger;
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
        try
        {
            var key = await _relationshipService.CreatePairingKeyAsync(GetUserId());
            return Ok(key);
        }
        catch (HttpRequestException ex) when (ex.StatusCode == System.Net.HttpStatusCode.TooManyRequests)
        {
            return StatusCode(429, new { message = ex.Message });
        }
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
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error joining relationship.");
            return StatusCode(500, new { message = "An internal error occurred." });
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

    [HttpPut("anniversary")]
    public async Task<IActionResult> UpdateAnniversary([FromBody] UpdateAnniversaryRequest req)
    {
        try
        {
            var rel = await _relationshipService.UpdateAnniversaryAsync(GetUserId(), req.AnniversaryDate);
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
    [EnableRateLimiting("AuthLimiter")]
    public async Task<IActionResult> Unpair()
    {
        await _relationshipService.UnpairAsync(GetUserId());
        return Ok();
    }

    [HttpPost("block")]
    [EnableRateLimiting("AuthLimiter")]
    public async Task<IActionResult> Block()
    {
        try
        {
            await _relationshipService.BlockCurrentPartnerAsync(GetUserId());
            return Ok();
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }
}
