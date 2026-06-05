using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Moment.Api.DTOs;
using Moment.Api.Services;
using System.Security.Claims;

namespace Moment.Api.Controllers;

[Authorize]
[ApiController]
[Route("api/v1/timeline")]
public class TimelineController : ControllerBase
{
    private readonly ITimelineService _timelineService;

    public TimelineController(ITimelineService timelineService)
    {
        _timelineService = timelineService;
    }

    [HttpGet]
    public async Task<IActionResult> GetTimeline([FromQuery] int page = 1, [FromQuery] int pageSize = 20)
    {
        var userId = GetUserId();
        var result = await _timelineService.GetTimelineAsync(userId, page, pageSize);
        return Ok(result);
    }

    [HttpPost("report")]
    public async Task<IActionResult> Report([FromBody] ReportRequest request)
    {
        var userId = GetUserId();
        var success = await _timelineService.ReportAsync(userId, request);
        if (!success) return BadRequest("Invalid report request");
        return Ok(new { success = true });
    }

    [HttpDelete("account")]
    public async Task<IActionResult> DeleteAccount()
    {
        var userId = GetUserId();
        var success = await _timelineService.DeleteAccountAsync(userId);
        if (!success) return BadRequest("Could not delete account");
        return Ok(new { success = true });
    }

    private Guid GetUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier);
        return Guid.Parse(userIdClaim!.Value);
    }
}
