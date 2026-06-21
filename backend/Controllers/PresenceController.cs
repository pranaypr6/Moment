using System;
using System.Net.Http;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Moment.Api.DTOs;
using Moment.Api.Services;

namespace Moment.Api.Controllers;

[ApiController]
[Route("api/v1/presence")]
[Authorize]
public class PresenceController : ControllerBase
{
    private readonly IPresenceService _presenceService;

    public PresenceController(IPresenceService presenceService)
    {
        _presenceService = presenceService;
    }

    [HttpPost("signal")]
    public async Task<IActionResult> SendPresenceSignal([FromBody] SendPresenceRequest req)
    {
        Console.WriteLine($"Received presence signal:");
        var uidString = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(uidString) || !Guid.TryParse(uidString, out var userId))
            return Unauthorized();

        try
        {
            var result = await _presenceService.SendPresenceSignalAsync(userId, req);
            Console.WriteLine($"Presence signal sent successfully.");
            return Ok(result);
        }
        catch (HttpRequestException ex) when (ex.StatusCode == System.Net.HttpStatusCode.TooManyRequests)
        {
            return StatusCode(429, new { message = "You're sending a lot of signals! Give it a little bit before sending another." });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }
}
