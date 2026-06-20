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
    public IActionResult GetUploadUrl([FromQuery] string fileName, [FromQuery] string contentType)
    {
        var uploadUrl = _storageService.GetPresignedUploadUrl(fileName, contentType);
        var publicUrl = _storageService.GetPublicUrl(fileName);
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
