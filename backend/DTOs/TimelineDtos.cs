namespace Moment.Api.DTOs;

public record ReportRequest(
    Guid? ReportedUserId,
    Guid? MomentId,
    string Reason
);

public record TimelineResponse(
    IEnumerable<MomentDto> Moments,
    int TotalCount
);
