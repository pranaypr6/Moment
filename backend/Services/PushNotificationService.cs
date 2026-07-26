using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using FirebaseAdmin.Messaging;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Moment.Api.Data;
using Moment.Api.DTOs;

namespace Moment.Api.Services;

public interface IPushNotificationService
{
    Task SendMomentNotificationAsync(Guid receiverUserId, MomentDto moment, string senderName);
    Task SendPresenceSignalAsync(Guid receiverUserId, PresenceSignalDto signal, string senderName);
    Task SendReactionNotificationAsync(Guid receiverUserId, Guid momentId, string senderName);
    Task SendVibeUpdateNotificationAsync(Guid receiverUserId, string senderName, string vibe);
}

public class FirebasePushNotificationService : IPushNotificationService
{
    private readonly MomentDbContext _context;
    private readonly ILogger<FirebasePushNotificationService> _logger;

    public FirebasePushNotificationService(MomentDbContext context, ILogger<FirebasePushNotificationService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task SendMomentNotificationAsync(Guid receiverUserId, MomentDto moment, string senderName)
    {
        var devices = await _context.Devices
            .Where(d => d.UserId == receiverUserId && !string.IsNullOrEmpty(d.FcmToken))
            .ToListAsync();

        if (!devices.Any())
        {
            _logger.LogInformation("No devices found for user {ReceiverUserId}", receiverUserId);
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
            _logger.LogInformation("Sent {SuccessCount} messages successfully. Failed: {FailureCount}", response.SuccessCount, response.FailureCount);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending push notifications.");
        }
    }

    public async Task SendPresenceSignalAsync(Guid receiverUserId, PresenceSignalDto signal, string senderName)
    {
        var devices = await _context.Devices
            .Where(d => d.UserId == receiverUserId && !string.IsNullOrEmpty(d.FcmToken))
            .ToListAsync();

        if (!devices.Any())
        {
            _logger.LogInformation("No devices found for user {ReceiverUserId}", receiverUserId);
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
                    { "signalType", "presence" },
                    { "presenceType", signal.Type.ToString() },
                    { "senderName", senderName },
                    { "relationshipId", signal.RelationshipId.ToString() },
                    { "createdAt", new DateTimeOffset(signal.CreatedAtUtc).ToUnixTimeMilliseconds().ToString() }
                }
            };
            messages.Add(message);
        }

        try
        {
            var response = await FirebaseMessaging.DefaultInstance.SendEachAsync(messages);
            _logger.LogInformation("Sent {SuccessCount} presence messages successfully. Failed: {FailureCount}", response.SuccessCount, response.FailureCount);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending presence push notifications.");
        }
    }

    public async Task SendReactionNotificationAsync(Guid receiverUserId, Guid momentId, string senderName)
    {
        var devices = await _context.Devices
            .Where(d => d.UserId == receiverUserId && !string.IsNullOrEmpty(d.FcmToken))
            .ToListAsync();

        if (!devices.Any()) return;

        var messages = devices.Select(device => new Message()
        {
            Token = device.FcmToken,
            Data = new Dictionary<string, string>()
            {
                { "signalType", "reaction" },
                { "momentId", momentId.ToString() },
                { "senderName", senderName }
            }
        }).ToList();

        try
        {
            await FirebaseMessaging.DefaultInstance.SendEachAsync(messages);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending reaction push notifications.");
        }
    }
    public async Task SendVibeUpdateNotificationAsync(Guid receiverUserId, string senderName, string vibe)
    {
        var devices = await _context.Devices
            .Where(d => d.UserId == receiverUserId && !string.IsNullOrEmpty(d.FcmToken))
            .ToListAsync();

        if (!devices.Any())
        {
            _logger.LogInformation("No devices found for user {ReceiverUserId}", receiverUserId);
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
                    { "signalType", "vibe" },
                    { "senderName", senderName },
                    { "vibe", vibe }
                }
            };
            messages.Add(message);
        }

        try
        {
            var response = await FirebaseMessaging.DefaultInstance.SendEachAsync(messages);
            _logger.LogInformation("Sent {SuccessCount} vibe messages successfully. Failed: {FailureCount}", response.SuccessCount, response.FailureCount);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending vibe push notifications.");
        }
    }
}
