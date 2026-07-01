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
    public IActionResult GetUploadUrl([FromQuery] string contentType)
    {
        if (contentType != "image/jpeg" && contentType != "image/png" && contentType != "image/webp")
        {
            return BadRequest("Invalid content type. Only JPEG, PNG, and WebP are allowed.");
        }

        var extension = contentType switch
        {
            "image/jpeg" => ".jpg",
            "image/png" => ".png",
            "image/webp" => ".webp",
            _ => ".jpg"
        };

        var secureFileName = $"{Guid.NewGuid()}{extension}";
        var uploadUrl = _storageService.GetPresignedUploadUrl(secureFileName, contentType);
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
            return BadRequest(ex.Message);
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
            return BadRequest(ex.Message);
        }
    }

    [HttpPut("{id}/favorite")]
    public async Task<IActionResult> ToggleFavorite(Guid id)
    {
        try
        {
            var moment = await _momentService.ToggleFavoriteAsync(GetUserId(), id);
            return Ok(moment);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }
}
