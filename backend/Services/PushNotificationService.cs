using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using FirebaseAdmin.Messaging;
using Microsoft.EntityFrameworkCore;
using Moment.Api.Data;
using Moment.Api.DTOs;

namespace Moment.Api.Services;

public interface IPushNotificationService
{
    Task SendMomentNotificationAsync(Guid receiverUserId, MomentDto moment, string senderName);
}

public class FirebasePushNotificationService : IPushNotificationService
{
    private readonly MomentDbContext _context;

    public FirebasePushNotificationService(MomentDbContext context)
    {
        _context = context;
    }

    public async Task SendMomentNotificationAsync(Guid receiverUserId, MomentDto moment, string senderName)
    {
        var devices = await _context.Devices
            .Where(d => d.UserId == receiverUserId && !string.IsNullOrEmpty(d.FcmToken))
            .ToListAsync();

        if (!devices.Any())
        {
            Console.WriteLine($"No devices found for user {receiverUserId}");
            return;
        }

        var messages = new List<Message>();
        foreach (var device in devices)
        {
            var message = new Message()
            {
                Token = device.FcmToken,
                Data = new Dictionary<string, string>()
                {
                    { "momentId", moment.Id.ToString() },
                    { "relationshipId", moment.RelationshipId.ToString() },
                    { "creatorId", moment.CreatorUserId.ToString() },
                    { "imageUrl", moment.ImageUrl },
                    { "thumbnailUrl", moment.ThumbnailUrl ?? "" },
                    { "note", moment.Note ?? "" },
                    { "wallpaperTarget", moment.WallpaperTarget.ToString() },
                    { "status", moment.Status.ToString() },
                    { "createdAt", new DateTimeOffset(moment.CreatedAt).ToUnixTimeMilliseconds().ToString() },
                    { "senderName", senderName }
                }
            };
            messages.Add(message);
        }

        try
        {
            var response = await FirebaseMessaging.DefaultInstance.SendEachAsync(messages);
            Console.WriteLine($"Sent {response.SuccessCount} messages successfully. Failed: {response.FailureCount}");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error sending push notifications: {ex.Message}");
        }
    }
}
