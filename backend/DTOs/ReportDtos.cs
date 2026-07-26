using System.ComponentModel.DataAnnotations;

namespace Moment.Api.DTOs;

public record CreateReportRequest(
    Guid? ReportedUserId,
    Guid? MomentId,
    [Required]
    [StringLength(500, MinimumLength = 1)]
    string Reason
);

public record ReportDto(
    Guid Id,
    Guid? ReportedUserId,
    Guid? MomentId,
    string Reason,
    DateTime CreatedAt
);
