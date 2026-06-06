using FirebaseAdmin.Messaging;
using Microsoft.EntityFrameworkCore;
using Moment.Api.Data;
using Moment.Api.DTOs;
using Moment.Api.Models;

namespace Moment.Api.Services;

public interface IMomentService
{
    Task<MomentDto?> SendMomentAsync(Guid senderId, SendMomentRequest request);
    Task<IEnumerable<MomentDto>> GetPendingMomentsAsync(Guid userId);
    Task<bool> UpdateMomentStatusAsync(Guid userId, Guid momentId, MomentStatus status);
    Task RegisterDeviceAsync(Guid userId, RegisterDeviceRequest request);
}

public class MomentService : IMomentService
{
    private readonly MomentDbContext _context;

    public MomentService(MomentDbContext context)
    {
        _context = context;
    }

    public async Task<MomentDto?> SendMomentAsync(Guid senderId, SendMomentRequest request)
    {
        // Check if active two-way connection exists
        var isConnected = await _context.UserConnections
            .AnyAsync(c => c.UserId == senderId && c.ConnectedUserId == request.ReceiverUserId);

        if (!isConnected) return null;

        var moment = new WallpaperMoment
        {
            Id = Guid.NewGuid(),
            SenderUserId = senderId,
            ReceiverUserId = request.ReceiverUserId,
            ImageUrl = request.ImageUrl,
            ThumbnailUrl = request.ThumbnailUrl,
            Note = request.Note,
            WallpaperTarget = request.WallpaperTarget,
            Status = MomentStatus.PENDING,
            CreatedAt = DateTime.UtcNow
        };

        _context.Moments.Add(moment);
        await _context.SaveChangesAsync();

        // Send FCM Notification
        await SendFcmNotificationAsync(moment);

        var sender = await _context.Users.FindAsync(senderId);
        var receiver = await _context.Users.FindAsync(request.ReceiverUserId);
        return MapToDto(moment, sender!, receiver!);
    }

    public async Task<IEnumerable<MomentDto>> GetPendingMomentsAsync(Guid userId)
    {
        var moments = await _context.Moments
            .Include(m => m.Sender)
            .Include(m => m.Receiver)
            .Where(m => m.ReceiverUserId == userId && m.Status == MomentStatus.PENDING)
            .OrderByDescending(m => m.CreatedAt)
            .ToListAsync();

        return moments.Select(m => MapToDto(m, m.Sender!, m.Receiver!));
    }

    public async Task<bool> UpdateMomentStatusAsync(Guid userId, Guid momentId, MomentStatus status)
    {
        var moment = await _context.Moments.FindAsync(momentId);
        if (moment == null || moment.ReceiverUserId != userId) return false;

        moment.Status = status;
        if (status == MomentStatus.DELIVERED) moment.DeliveredAt = DateTime.UtcNow;
        if (status == MomentStatus.APPLIED) moment.AppliedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return true;
    }

    public async Task RegisterDeviceAsync(Guid userId, RegisterDeviceRequest request)
    {
        var device = await _context.Devices.FirstOrDefaultAsync(d => d.FcmToken == request.FcmToken);

        if (device == null)
        {
            device = new Device
            {
                Id = Guid.NewGuid(),
                UserId = userId,
                FcmToken = request.FcmToken,
                Platform = request.Platform,
                DeviceName = request.DeviceName,
                CreatedAt = DateTime.UtcNow
            };
            _context.Devices.Add(device);
        }
        else
        {
            device.UserId = userId;
            device.Platform = request.Platform;
            device.DeviceName = request.DeviceName;
            device.LastSeenAt = DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();
    }

    private async Task SendFcmNotificationAsync(WallpaperMoment moment)
    {
        var devices = await _context.Devices
            .Where(d => d.UserId == moment.ReceiverUserId)
            .ToListAsync();

        if (!devices.Any()) return;

        var sender = await _context.Users.FindAsync(moment.SenderUserId);

        var message = new MulticastMessage
        {
            Tokens = devices.Select(d => d.FcmToken).ToList(),
            Data = new Dictionary<string, string>
            {
                { "type", "NEW_MOMENT" },
                { "momentId", moment.Id.ToString() },
                { "senderName", sender?.DisplayName ?? "Someone" },
                { "imageUrl", moment.ImageUrl },
                { "thumbnailUrl", moment.ThumbnailUrl ?? "" },
                { "note", moment.Note ?? "" },
                { "wallpaperTarget", moment.WallpaperTarget.ToString() }
            },
            Android = new AndroidConfig
            {
                Priority = Priority.High
            }
        };

        try
        {
            await FirebaseMessaging.DefaultInstance.SendMulticastAsync(message);
        }
        catch (Exception)
        {
            // Log FCM error
        }
    }

    private MomentDto MapToDto(WallpaperMoment m, User sender, User receiver) => new MomentDto(
        m.Id,
        new UserDto(sender.Id, sender.Email, sender.Username, sender.DisplayName, sender.ProfilePictureUrl, sender.Bio),
        new UserDto(receiver.Id, receiver.Email, receiver.Username, receiver.DisplayName, receiver.ProfilePictureUrl, receiver.Bio),
        m.ImageUrl,
        m.ThumbnailUrl,
        m.Note,
        m.WallpaperTarget,
        m.Status,
        m.CreatedAt
    );
}
