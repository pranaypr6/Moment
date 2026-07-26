namespace Moment.Api.DTOs;

using System.ComponentModel.DataAnnotations;
using Moment.Api.Models;

public record MomentDto(
    Guid Id,
    Guid RelationshipId,
    Guid CreatorUserId,
    string ImageUrl,
    string? ThumbnailUrl,
    string? Note,
    WallpaperTarget WallpaperTarget,
    bool IsFavorite,
    MomentStatus Status,
    DateTime CreatedAt,
    DateTime? DeliveredAt,
    DateTime? AppliedAt
);

public record CreateMomentRequest(
    [Required]
    [StringLength(2048)]
    string ImageUrl,
    [StringLength(2048)]
    string? ThumbnailUrl,
    [StringLength(250)]
    string? Note,
    WallpaperTarget WallpaperTarget
);

public record PaginatedResponse<T>(
    IEnumerable<T> Items,
    bool HasMore,
    string? NextCursor
);

public record UploadUrlResponse(
    string UploadUrl,
    string PublicUrl
);

public record FavoriteRequest(
    bool IsFavorite
);
