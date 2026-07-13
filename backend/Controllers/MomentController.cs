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
[Route("api/moments")]
public class MomentController : ControllerBase
{
    private readonly IMomentService _momentService;
    private readonly IStorageService _storageService;

    public MomentController(IMomentService momentService, IStorageService storageService)
    {
        _momentService = momentService;
        _storageService = storageService;
    }

    private Guid GetUserId() => Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

    [HttpGet("upload-url")]
    [EnableRateLimiting("EmotionalLimiter")]
    public async Task<IActionResult> GetUploadUrl([FromServices] IRelationshipService relationshipService, [FromQuery] string contentType, [FromQuery] long contentLength)
    {
        var rel = await relationshipService.GetCurrentRelationshipAsync(GetUserId());
        if (rel == null) return Forbid();
        if (contentType != "image/jpeg" && contentType != "image/png" && contentType != "image/webp")
        {
            return BadRequest("Invalid content type. Only JPEG, PNG, and WebP are allowed.");
        }

        const long maxFileSize = 10 * 1024 * 1024; // 10MB
        if (contentLength <= 0 || contentLength > maxFileSize)
        {
            return BadRequest("Invalid file size. Must be between 1 byte and 10MB.");
        }

        var extension = contentType switch
        {
            "image/jpeg" => ".jpg",
            "image/png" => ".png",
            "image/webp" => ".webp",
            _ => ".jpg"
        };

        var secureFileName = $"{Guid.NewGuid()}{extension}";
        var uploadUrl = _storageService.GetPresignedUploadUrl(secureFileName, contentType, contentLength);
        var publicUrl = _storageService.GetPublicUrl(secureFileName);
        return Ok(new UploadUrlResponse(uploadUrl, publicUrl));
    }

    [HttpGet("~/api/relationship/{relationshipId}/scrapbook")]
    public async Task<IActionResult> GetScrapbook(Guid relationshipId, [FromQuery] int limit = 20, [FromQuery] string? cursor = null)
    {
        try
        {
            var res = await _momentService.GetScrapbookAsync(GetUserId(), relationshipId, limit, cursor);
            return Ok(res);
        }
        catch (InvalidOperationException)
        {
            return Forbid();
        }
    }

    [HttpGet("pending")]
    public async Task<IActionResult> GetPending()
    {
        try
        {
            var moments = await _momentService.GetPendingMomentsAsync(GetUserId());
            return Ok(moments);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error getting pending moments: {ex}");
            return StatusCode(500, "An internal error occurred.");
        }
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreateMomentRequest req)
    {
        try
        {
            var moment = await _momentService.CreateMomentAsync(GetUserId(), req);
            return Ok(moment);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message); // Kept because this throws safe domain errors like "No active relationship"
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error creating moment: {ex}");
            return StatusCode(500, "An internal error occurred.");
        }
    }

    [HttpPut("{id}/favorite")]
    public async Task<IActionResult> SetFavorite(Guid id, [FromBody] FavoriteRequest req)
    {
        try
        {
            var moment = await _momentService.SetFavoriteAsync(GetUserId(), id, req.IsFavorite);
            return Ok(moment);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error favoriting moment: {ex}");
            return StatusCode(500, "An internal error occurred.");
        }
    }
}
