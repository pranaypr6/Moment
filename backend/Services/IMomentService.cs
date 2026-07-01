using System;
using System.Threading.Tasks;
using Moment.Api.DTOs;

namespace Moment.Api.Services;

public interface IMomentService
{
    Task<PaginatedResponse<MomentDto>> GetScrapbookAsync(Guid userId, Guid relationshipId, int limit, string? cursor);
    Task<MomentDto> CreateMomentAsync(Guid userId, CreateMomentRequest req);
    Task<System.Collections.Generic.List<MomentDto>> GetPendingMomentsAsync(Guid userId);
    Task<MomentDto> ToggleFavoriteAsync(Guid userId, Guid momentId);
}
