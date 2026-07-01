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

    public MomentService(MomentDbContext context, IPushNotificationService pushNotificationService)
    {
        _context = context;
        _pushNotificationService = pushNotificationService;
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
            .Include(m => m.Relationship)
            .Where(m => m.RelationshipId == relationshipId)
            .OrderByDescending(m => m.CreatedAt)
            .AsQueryable();

        if (!string.IsNullOrEmpty(cursor) && DateTime.TryParse(cursor, out var cursorDate))
        {
            // Note: Ensuring precision matches DB usually means UTC
            query = query.Where(m => m.CreatedAt < cursorDate.ToUniversalTime());
        }

        var items = await query.Take(limit + 1).ToListAsync();
        var hasMore = items.Count > limit;
        if (hasMore) items.RemoveAt(limit);

        var nextCursor = hasMore ? items.Last().CreatedAt.ToString("o") : null;
        var dtos = items.Select(m => MapToDto(m, userId));

        return new PaginatedResponse<MomentDto>(dtos, hasMore, nextCursor);
    }

    public async Task<MomentDto> CreateMomentAsync(Guid userId, CreateMomentRequest req)
    {
        var rel = await _context.Relationships
            .FirstOrDefaultAsync(r => (r.Partner1Id == userId || r.Partner2Id == userId) && r.Status == RelationshipStatus.Active);

        if (rel == null) throw new InvalidOperationException("No active relationship to share to.");

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

    public async Task<MomentDto> ToggleFavoriteAsync(Guid userId, Guid momentId)
    {
        var moment = await _context.Moments
            .Include(m => m.Relationship)
            .FirstOrDefaultAsync(m => m.Id == momentId && (m.Relationship!.Partner1Id == userId || m.Relationship.Partner2Id == userId));

        if (moment == null) throw new InvalidOperationException("Moment not found.");

        if (moment.Relationship!.Partner1Id == userId)
        {
            moment.FavoritedByPartner1 = !moment.FavoritedByPartner1;
        }
        else
        {
            moment.FavoritedByPartner2 = !moment.FavoritedByPartner2;
        }

        await _context.SaveChangesAsync();
        return MapToDto(moment, userId);
    }
}
