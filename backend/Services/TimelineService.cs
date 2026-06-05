using Microsoft.EntityFrameworkCore;
using Moment.Api.Data;
using Moment.Api.DTOs;
using Moment.Api.Models;

namespace Moment.Api.Services;

public interface ITimelineService
{
    Task<TimelineResponse> GetTimelineAsync(Guid userId, int page, int pageSize);
    Task<bool> ReportAsync(Guid reporterId, ReportRequest request);
    Task<bool> DeleteAccountAsync(Guid userId);
}

public class TimelineService : ITimelineService
{
    private readonly MomentDbContext _context;

    public TimelineService(MomentDbContext context)
    {
        _context = context;
    }

    public async Task<TimelineResponse> GetTimelineAsync(Guid userId, int page, int pageSize)
    {
        var query = _context.Moments
            .Include(m => m.Sender)
            .Where(m => m.ReceiverUserId == userId || m.SenderUserId == userId)
            .OrderByDescending(m => m.CreatedAt);

        var totalCount = await query.CountAsync();
        var moments = await query
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync();

        return new TimelineResponse(
            moments.Select(m => MapToDto(m, m.Sender!)),
            totalCount
        );
    }

    public async Task<bool> ReportAsync(Guid reporterId, ReportRequest request)
    {
        if (request.ReportedUserId == null && request.MomentId == null) return false;

        var report = new Report
        {
            Id = Guid.NewGuid(),
            ReporterUserId = reporterId,
            ReportedUserId = request.ReportedUserId,
            MomentId = request.MomentId,
            Reason = request.Reason,
            CreatedAt = DateTime.UtcNow
        };

        _context.Reports.Add(report);
        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<bool> DeleteAccountAsync(Guid userId)
    {
        using var transaction = await _context.Database.BeginTransactionAsync();
        try
        {
            var user = await _context.Users.FindAsync(userId);
            if (user == null) return false;

            // Delete Connections
            var connections = await _context.Connections
                .Where(c => c.SenderUserId == userId || c.ReceiverUserId == userId).ToListAsync();
            _context.Connections.RemoveRange(connections);

            // Delete Devices
            var devices = await _context.Devices.Where(d => d.UserId == userId).ToListAsync();
            _context.Devices.RemoveRange(devices);

            // Delete Reports (where user is reporter)
            var reports = await _context.Reports.Where(r => r.ReporterUserId == userId).ToListAsync();
            _context.Reports.RemoveRange(reports);

            // Delete Moments
            // Note: In production, we would queue background tasks to delete these from R2.
            var moments = await _context.Moments
                .Where(m => m.SenderUserId == userId || m.ReceiverUserId == userId).ToListAsync();
            _context.Moments.RemoveRange(moments);

            // Finally, delete the User
            _context.Users.Remove(user);
            
            await _context.SaveChangesAsync();
            await transaction.CommitAsync();
            return true;
        }
        catch (Exception)
        {
            await transaction.RollbackAsync();
            return false;
        }
    }

    private MomentDto MapToDto(WallpaperMoment m, User sender) => new MomentDto(
        m.Id,
        new UserDto(sender.Id, sender.Email, sender.Username, sender.DisplayName, sender.ProfilePictureUrl, sender.Bio),
        m.ImageUrl,
        m.ThumbnailUrl,
        m.Note,
        m.WallpaperTarget,
        m.Status,
        m.CreatedAt
    );
}
