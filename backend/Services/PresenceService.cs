using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Moment.Api.Data;
using Moment.Api.DTOs;
using Moment.Api.Models;

namespace Moment.Api.Services;

public interface IPresenceService
{
    Task<PresenceSignalDto> SendPresenceSignalAsync(Guid userId, SendPresenceRequest req);
}

public class PresenceService : IPresenceService
{
    private readonly MomentDbContext _context;
    private readonly IPushNotificationService _pushService;
    private readonly IConfiguration _config;

    public PresenceService(MomentDbContext context, IPushNotificationService pushService, IConfiguration config)
    {
        _context = context;
        _pushService = pushService;
        _config = config;
    }

    public async Task<PresenceSignalDto> SendPresenceSignalAsync(Guid userId, SendPresenceRequest req)
    {
        var rel = await _context.Relationships
            .Include(r => r.Partner1)
            .Include(r => r.Partner2)
            .FirstOrDefaultAsync(r => r.Id == req.RelationshipId && (r.Partner1Id == userId || r.Partner2Id == userId));

        if (rel == null) throw new InvalidOperationException("Relationship not found.");

        var partnerId = rel.Partner1Id == userId ? rel.Partner2Id : rel.Partner1Id;
        var senderUser = rel.Partner1Id == userId ? rel.Partner1 : rel.Partner2;
        var senderName = senderUser?.Username ?? "Someone";

        // Rate Limiting Logic
        var limitPerHour = _config.GetValue<int>("RateLimits:EmotionalActionPerHour", 500);
        var limitPerDay = _config.GetValue<int>("RateLimits:EmotionalActionPerDay", 1000);

        var now = DateTime.UtcNow;
        var oneHourAgo = now.AddHours(-1);
        var oneDayAgo = now.AddDays(-1);

        var baseQuery = _context.PresenceSignals
            .Where(p => p.SenderUserId == userId && p.RelationshipId == req.RelationshipId && p.Type == req.Type);

        var countLastDay = await baseQuery
            .Where(p => p.CreatedAtUtc >= oneDayAgo)
            .CountAsync();

        var countLastHour = await baseQuery
            .Where(p => p.CreatedAtUtc >= oneHourAgo)
            .CountAsync();

        if (countLastHour >= limitPerHour || countLastDay >= limitPerDay)
        {
            throw new HttpRequestException("Rate limit exceeded for presence signals", null, System.Net.HttpStatusCode.TooManyRequests);
        }

        var signal = new PresenceSignal
        {
            Id = Guid.NewGuid(),
            RelationshipId = req.RelationshipId,
            SenderUserId = userId,
            ReceiverUserId = partnerId,
            Type = req.Type,
            CreatedAtUtc = now
        };

        _context.PresenceSignals.Add(signal);
        await _context.SaveChangesAsync();

        var dto = new PresenceSignalDto
        {
            Id = signal.Id,
            RelationshipId = signal.RelationshipId,
            SenderUserId = signal.SenderUserId,
            ReceiverUserId = signal.ReceiverUserId,
            Type = signal.Type,
            CreatedAtUtc = signal.CreatedAtUtc
        };

        await _pushService.SendPresenceSignalAsync(partnerId, dto, senderName);

        return dto;
    }
}
