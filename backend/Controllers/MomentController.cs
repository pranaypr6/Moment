using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Moment.Api.DTOs;
using Moment.Api.Services;
using System.Security.Claims;

namespace Moment.Api.Controllers;

[Authorize]
[ApiController]
[Route("api/v1/moments")]
public class MomentController : ControllerBase
{
    private readonly IMomentService _momentService;
    private readonly IStorageService _storageService;

    public MomentController(IMomentService momentService, IStorageService storageService)
    {
        _momentService = momentService;
        _storageService = storageService;
    }

    [HttpPost]
    public async Task<IActionResult> SendMoment([FromBody] SendMomentRequest request)
    {
        var senderId = GetUserId();
        try
        {
            var result = await _momentService.SendMomentAsync(senderId, request);
            if (result == null) return BadRequest("Could not send moment. Check connection status.");
            return Ok(result);
        }
        catch (InvalidOperationException ex)
        {
            return StatusCode(429, ex.Message);
        }
    }

    [HttpGet("pending")]
    public async Task<IActionResult> GetPendingMoments()
    {
        var userId = GetUserId();
        var result = await _momentService.GetPendingMomentsAsync(userId);
        return Ok(result);
    }

    [HttpPatch("{momentId}/status")]
    public async Task<IActionResult> UpdateStatus(Guid momentId, [FromBody] UpdateMomentStatusRequest request)
    {
        var userId = GetUserId();
        var success = await _momentService.UpdateMomentStatusAsync(userId, momentId, request.Status);
        if (!success) return BadRequest("Could not update moment status");
        return Ok(new { success = true });
    }

    [HttpPost("register-device")]
    public async Task<IActionResult> RegisterDevice([FromBody] RegisterDeviceRequest request)
    {
        var userId = GetUserId();
        await _momentService.RegisterDeviceAsync(userId, request);
        return Ok(new { success = true });
    }

    [HttpGet("upload-url")]
    public async Task<IActionResult> GetUploadUrl([FromQuery] string fileName, [FromQuery] string contentType)
    {
        var uploadUrl = _storageService.GetPresignedUploadUrl(fileName, contentType);
        var publicUrl = _storageService.GetPublicUrl(fileName);
        return Ok(new UploadUrlResponse(uploadUrl, publicUrl));
    }

    private Guid GetUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier);
        return Guid.Parse(userIdClaim!.Value);
    }
}
