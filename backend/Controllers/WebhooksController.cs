using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Moment.Api.Data;
using Moment.Api.Models;

namespace Moment.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class WebhooksController : ControllerBase
{
    private readonly MomentDbContext _context;
    private readonly IConfiguration _configuration;
    private readonly ILogger<WebhooksController> _logger;

    public WebhooksController(MomentDbContext context, IConfiguration configuration, ILogger<WebhooksController> logger)
    {
        _context = context;
        _configuration = configuration;
        _logger = logger;
    }

    [HttpPost("revenuecat")]
    public async Task<IActionResult> RevenueCatWebhook([FromBody] RevenueCatWebhookPayload payload)
    {
        // 1. Verify Authorization Header
        var expectedSecret = _configuration["RevenueCat:WebhookSecret"];
        if (string.IsNullOrEmpty(expectedSecret))
        {
            _logger.LogWarning("RevenueCat:WebhookSecret is not configured in appsettings.json");
            return StatusCode(500, "Server misconfiguration");
        }

        var authHeader = Request.Headers["Authorization"].FirstOrDefault();
        if (authHeader != $"Bearer {expectedSecret}")
        {
            _logger.LogWarning("Unauthorized webhook attempt");
            return Unauthorized();
        }

        if (payload?.Event == null)
        {
            return BadRequest("Invalid payload");
        }

        var rcEvent = payload.Event;
        _logger.LogInformation("Received RevenueCat webhook: {Type} for user {UserId}", rcEvent.Type, rcEvent.AppUserId);

        // 2. Parse User ID
        if (!Guid.TryParse(rcEvent.AppUserId, out var userId))
        {
            _logger.LogWarning("Invalid AppUserId format from RevenueCat: {UserId}", rcEvent.AppUserId);
            // Return OK so RevenueCat doesn't keep retrying this malformed request
            return Ok();
        }

        // 3. Find User
        var user = await _context.Users.FindAsync(userId);
        if (user == null)
        {
            _logger.LogWarning("User not found for RevenueCat webhook: {UserId}", userId);
            return Ok(); // User might have been deleted, don't retry
        }

        // 4. Handle Event Types
        bool isPremiumEvent = false;
        bool isRevokeEvent = false;

        switch (rcEvent.Type)
        {
            case "INITIAL_PURCHASE":
            case "RENEWAL":
            case "UNCANCELLATION":
            case "NON_RENEWING_PURCHASE":
            case "PRODUCT_CHANGE":
                isPremiumEvent = true;
                break;
            case "EXPIRATION":
            case "CANCELLATION": // Cancellation means they turned off auto-renew, but they might still have time left. We rely on EXPIRATION to actually revoke.
                // However, some implementations revoke on CANCELLATION if it's an immediate refund. 
                // We'll revoke strictly on EXPIRATION or BILLING_ISSUE.
                if (rcEvent.Type == "EXPIRATION")
                {
                    isRevokeEvent = true;
                }
                break;
            case "BILLING_ISSUE":
                isRevokeEvent = true;
                break;
            default:
                _logger.LogInformation("Ignored RevenueCat event type: {Type}", rcEvent.Type);
                return Ok();
        }

        // Update user's premium status
        if (isPremiumEvent)
        {
            user.IsPremium = true;
            user.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
            _logger.LogInformation("Granted Premium to user {UserId}", userId);
        }
        else if (isRevokeEvent)
        {
            user.IsPremium = false;
            user.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
            _logger.LogInformation("Revoked Premium from user {UserId}", userId);
        }

        return Ok();
    }
}
