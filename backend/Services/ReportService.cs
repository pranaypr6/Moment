using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Moment.Api.Data;
using Moment.Api.DTOs;
using Moment.Api.Models;

namespace Moment.Api.Services;

public interface IReportService
{
    Task<ReportDto> CreateReportAsync(Guid reporterUserId, CreateReportRequest request);
}

public class ReportService : IReportService
{
    private readonly MomentDbContext _context;
    private readonly ILogger<ReportService> _logger;

    public ReportService(MomentDbContext context, ILogger<ReportService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<ReportDto> CreateReportAsync(Guid reporterUserId, CreateReportRequest request)
    {
        if (request.ReportedUserId == null && request.MomentId == null)
        {
            throw new InvalidOperationException("A report must reference either a user or a moment.");
        }

        // There's no user directory/search in this app (MVP scope: pairing-only discovery),
        // so the only user a reporter can ever legitimately report is their current or a
        // former partner. Validate that here to prevent reporting arbitrary user IDs.
        if (request.ReportedUserId.HasValue)
        {
            var isPartner = await _context.Relationships.AnyAsync(r =>
                ((r.Partner1Id == reporterUserId && r.Partner2Id == request.ReportedUserId) ||
                 (r.Partner2Id == reporterUserId && r.Partner1Id == request.ReportedUserId)));

            if (!isPartner)
            {
                throw new InvalidOperationException("You can only report a current or former partner.");
            }
        }

        // Similarly, a moment can only be reported by someone who is actually part of
        // the relationship it belongs to.
        if (request.MomentId.HasValue)
        {
            var momentVisible = await _context.Moments
                .Include(m => m.Relationship)
                .AnyAsync(m => m.Id == request.MomentId &&
                    (m.Relationship!.Partner1Id == reporterUserId || m.Relationship.Partner2Id == reporterUserId));

            if (!momentVisible)
            {
                throw new InvalidOperationException("Moment not found or access denied.");
            }
        }

        var report = new Report
        {
            Id = Guid.NewGuid(),
            ReporterUserId = reporterUserId,
            ReportedUserId = request.ReportedUserId,
            MomentId = request.MomentId,
            Reason = request.Reason,
            CreatedAt = DateTime.UtcNow
        };

        _context.Reports.Add(report);
        await _context.SaveChangesAsync();

        _logger.LogWarning(
            "Report created: Id={ReportId} Reporter={ReporterId} ReportedUser={ReportedUserId} Moment={MomentId}",
            report.Id, reporterUserId, request.ReportedUserId, request.MomentId);

        return new ReportDto(report.Id, report.ReportedUserId, report.MomentId, report.Reason, report.CreatedAt);
    }
}
