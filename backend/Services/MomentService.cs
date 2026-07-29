using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Moment.Api.Data;
using Moment.Api.DTOs;
using Moment.Api.Models;

namespace Moment.Api.Services;

public class MomentService : IMomentService
{
    private readonly MomentDbContext _context;
    private readonly IPushNotificationService _pushNotificationService;
    private readonly IStorageService _storageService;
    private readonly IConfiguration _configuration;

    public MomentService(MomentDbContext context, IPushNotificationService pushNotificationService, IStorageService storageService, IConfiguration configuration)
    {
        _context = context;
        _pushNotificationService = pushNotificationService;
        _storageService = storageService;
        _configuration = configuration;
    }

    private MomentDto MapToDto(WallpaperMoment m, Guid callerId)
    {
        // Spec: The API layer should derive Favorite for display purposes if either value is true
        bool isFavorite = m.FavoritedByPartner1 || m.FavoritedByPartner2;

        return new MomentDto(
            m.Id,
            m.RelationshipId,
            m.CreatorUserId,
            m.ImageUrl,
            m.ThumbnailUrl,
            m.Note,
            m.WallpaperTarget,
            isFavorite,
            m.Status,
            m.CreatedAt,
            m.DeliveredAt,
            m.AppliedAt
        );
    }

    public async Task<PaginatedResponse<MomentDto>> GetScrapbookAsync(Guid userId, Guid relationshipId, int limit, string? cursor)
    {
        var rel = await _context.Relationships
            .FirstOrDefaultAsync(r => r.Id == relationshipId && (r.Partner1Id == userId || r.Partner2Id == userId));

        if (rel == null) throw new InvalidOperationException("Relationship not found or access denied.");

        var query = _context.Moments
            .AsNoTracking()
            .Include(m => m.Relationship)
            .Where(m => m.RelationshipId == relationshipId)
            .OrderByDescending(m => m.CreatedAt)
            .ThenByDescending(m => m.Id)
            .AsQueryable();

        if (!string.IsNullOrEmpty(cursor))
        {
            var parts = cursor.Split('_');
            if (parts.Length == 2 && DateTime.TryParse(parts[0], out var cursorDate) && Guid.TryParse(parts[1], out var cursorId))
            {
                var utcDate = cursorDate.ToUniversalTime();
                query = query.Where(m => m.CreatedAt < utcDate || (m.CreatedAt == utcDate && m.Id.CompareTo(cursorId) < 0));
            }
        }

        var items = await query.Take(limit + 1).ToListAsync();
        var hasMore = items.Count > limit;
        if (hasMore) items.RemoveAt(limit);

        var nextCursor = hasMore ? $"{items.Last().CreatedAt:o}_{items.Last().Id}" : null;
        var dtos = items.Select(m => MapToDto(m, userId));

        return new PaginatedResponse<MomentDto>(dtos, hasMore, nextCursor);
    }

    public async Task<MomentDto> CreateMomentAsync(Guid userId, CreateMomentRequest req)
    {
        var rel = await _context.Relationships
            .FirstOrDefaultAsync(r => (r.Partner1Id == userId || r.Partner2Id == userId) && r.Status == RelationshipStatus.Active);

        if (rel == null) throw new InvalidOperationException("No active relationship to share to.");

        if (rel.Partner1PausedAt.HasValue || rel.Partner2PausedAt.HasValue)
        {
            throw new InvalidOperationException("Moments are paused by partner for now.");
        }

        // Rate limiting: enforce the configured per-hour/per-day moment limits.
        var hourlyLimit = _configuration.GetValue<int>("MomentLimits:HourlyLimit", 5);
        var dailyLimit = _configuration.GetValue<int>("MomentLimits:DailyLimit", 20);
        var now = DateTime.UtcNow;
        var oneHourAgo = now.AddHours(-1);
        var oneDayAgo = now.AddDays(-1);

        var momentsLastDay = await _context.Moments
            .Where(m => m.CreatorUserId == userId && m.CreatedAt >= oneDayAgo)
            .CountAsync();
        var momentsLastHour = await _context.Moments
            .Where(m => m.CreatorUserId == userId && m.CreatedAt >= oneHourAgo)
            .CountAsync();

        if (momentsLastHour >= hourlyLimit || momentsLastDay >= dailyLimit)
        {
            throw new HttpRequestException("You've sent a lot of moments! Give it a little while before sending another.", null, System.Net.HttpStatusCode.TooManyRequests);
        }

        // Only accept image/thumbnail URLs that point at our own storage domain -
        // never trust an arbitrary client-supplied host (would let a sender point
        // their partner's device at an attacker-controlled URL via a trusted notification).
        var publicUrlPrefix = (_configuration["Cloudflare:PublicUrl"] ?? "").TrimEnd('/');
        if (string.IsNullOrEmpty(publicUrlPrefix) || !req.ImageUrl.StartsWith(publicUrlPrefix, StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException("Image URL must point to the app's own storage.");
        }
        if (!string.IsNullOrEmpty(req.ThumbnailUrl) && !req.ThumbnailUrl.StartsWith(publicUrlPrefix, StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException("Thumbnail URL must point to the app's own storage.");
        }

        var uri = new Uri(req.ImageUrl);
        var fileName = uri.Segments.Last();

        var headerBytes = await _storageService.GetFileHeaderBytesAsync(fileName, 16);
        
        bool isValidImage = false;
        if (headerBytes.Length >= 4)
        {
            // JPEG: FF D8 FF
            if (headerBytes[0] == 0xFF && headerBytes[1] == 0xD8 && headerBytes[2] == 0xFF) isValidImage = true;
            // PNG: 89 50 4E 47
            else if (headerBytes.Length >= 8 && headerBytes[0] == 0x89 && headerBytes[1] == 0x50 && headerBytes[2] == 0x4E && headerBytes[3] == 0x47) isValidImage = true;
            // WEBP: RIFF ... WEBP
            else if (headerBytes.Length >= 12 && 
                     headerBytes[0] == 0x52 && headerBytes[1] == 0x49 && headerBytes[2] == 0x46 && headerBytes[3] == 0x46 &&
                     headerBytes[8] == 0x57 && headerBytes[9] == 0x45 && headerBytes[10] == 0x42 && headerBytes[11] == 0x50) isValidImage = true;
        }

        if (!isValidImage)
        {
            await _storageService.DeleteFileAsync(fileName);
            throw new InvalidOperationException("Invalid file format. Only JPEG, PNG, and WebP images are allowed.");
        }

        var partnerId = rel.Partner1Id == userId ? rel.Partner2Id : rel.Partner1Id;

        var moment = new WallpaperMoment
        {
            RelationshipId = rel.Id,
            CreatorUserId = userId,
            ReceiverUserId = partnerId, // Phase 1 safe migration
            ImageUrl = req.ImageUrl,
            ThumbnailUrl = req.ThumbnailUrl,
            Note = req.Note,
            WallpaperTarget = req.WallpaperTarget,
            Status = MomentStatus.PENDING,
            CreatedAt = DateTime.UtcNow
        };

        _context.Moments.Add(moment);
        await _context.SaveChangesAsync();

        moment.Relationship = rel; // for mapping
        var dto = MapToDto(moment, userId);

        // Fetch sender's name for the notification
        var sender = await _context.Users.FindAsync(userId);
        var senderName = sender?.DisplayName ?? sender?.Username ?? "Your partner";

        // Send push notification
        await _pushNotificationService.SendMomentNotificationAsync(partnerId, dto, senderName);

        return dto;
    }

    public async Task<System.Collections.Generic.List<MomentDto>> GetPendingMomentsAsync(Guid userId)
    {
        var moments = await _context.Moments
            .Include(m => m.Relationship)
            .Where(m => m.ReceiverUserId == userId && m.Status == MomentStatus.PENDING)
            .OrderBy(m => m.CreatedAt)
            .ToListAsync();

        // Let's mark them as delivered since the user just fetched them.
        var now = DateTime.UtcNow;
        foreach (var m in moments)
        {
            m.Status = MomentStatus.DELIVERED;
            m.DeliveredAt = now;
        }

        if (moments.Any())
        {
            await _context.SaveChangesAsync();
        }

        return moments.Select(m => MapToDto(m, userId)).ToList();
    }

    public async Task MarkAppliedAsync(Guid userId, Guid momentId)
    {
        // The only place that used to move a moment off PENDING was GetPendingMomentsAsync,
        // triggered lazily whenever a client happened to poll /pending - a moment delivered
        // live via FCM (the normal, fast path) never told the server it arrived at all. That
        // meant the server had no durable record of "already handled," so if the receiving
        // device's local dedup state was ever lost (reinstall, app data cleared, a fresh
        // signed build replacing a debug install), the next /pending poll would treat an
        // already-applied moment as brand new and redeliver it - full wallpaper re-apply plus
        // a duplicate "X left you something" notification for something that happened hours
        // or days ago. This lets the client that actually applied the wallpaper confirm it,
        // so the server becomes the source of truth instead of relying solely on the
        // receiving device's own local database staying intact forever.
        var moment = await _context.Moments
            .FirstOrDefaultAsync(m => m.Id == momentId && m.ReceiverUserId == userId);

        if (moment == null) return; // Not this user's moment (or already gone) - nothing to do.

        // Idempotent: safe to call more than once (e.g. once from the live-FCM path and again
        // from a later resync of the same moment) - just keep the fields consistent.
        if (moment.Status != MomentStatus.APPLIED)
        {
            moment.Status = MomentStatus.APPLIED;
        }
        moment.AppliedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
    }

    public async Task<MomentDto> SetFavoriteAsync(Guid userId, Guid momentId, bool isFavorite)
    {
        var moment = await _context.Moments
            .Include(m => m.Relationship)
            .FirstOrDefaultAsync(m => m.Id == momentId && (m.Relationship!.Partner1Id == userId || m.Relationship.Partner2Id == userId));

        if (moment == null) throw new InvalidOperationException("Moment not found.");

        bool isAddingFavorite = false;

        if (moment.Relationship!.Partner1Id == userId)
        {
            isAddingFavorite = isFavorite && !moment.FavoritedByPartner1;
            moment.FavoritedByPartner1 = isFavorite;
        }
        else
        {
            isAddingFavorite = isFavorite && !moment.FavoritedByPartner2;
            moment.FavoritedByPartner2 = isFavorite;
        }

        await _context.SaveChangesAsync();
        var dto = MapToDto(moment, userId);

        if (isAddingFavorite)
        {
            var partnerId = moment.Relationship.Partner1Id == userId ? moment.Relationship.Partner2Id : moment.Relationship.Partner1Id;
            var sender = await _context.Users.FindAsync(userId);
            var senderName = sender?.DisplayName ?? sender?.Username ?? "Your partner";
            
            await _pushNotificationService.SendReactionNotificationAsync(partnerId, moment.Id, senderName);
        }

        return dto;
    }
}
