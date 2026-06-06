namespace Moment.Api.DTOs;

using Moment.Api.Models;

public record SendMomentRequest(
    Guid ReceiverUserId,
    string ImageUrl,
    string? ThumbnailUrl,
    string? Note,
    WallpaperTarget WallpaperTarget
);

public record MomentDto(
    Guid Id,
    UserDto Sender,
    UserDto Receiver,
    string ImageUrl,
    string? ThumbnailUrl,
    string? Note,
    WallpaperTarget WallpaperTarget,
    MomentStatus Status,
    DateTime CreatedAt
);

public record UploadUrlResponse(string UploadUrl, string PublicUrl);

public record RegisterDeviceRequest(string FcmToken, string? Platform, string? DeviceName);

public record UpdateMomentStatusRequest(MomentStatus Status);
