using System;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Moment.Api.Data;
using Moment.Api.DTOs;
using Moment.Api.Models;

namespace Moment.Api.Services;

public interface IDeviceService
{
    Task RegisterDeviceAsync(Guid userId, RegisterDeviceRequest request);
}

public class DeviceService : IDeviceService
{
    private readonly MomentDbContext _context;

    public DeviceService(MomentDbContext context)
    {
        _context = context;
    }

    public async Task RegisterDeviceAsync(Guid userId, RegisterDeviceRequest request)
    {
        var existingDevice = await _context.Devices
            .FirstOrDefaultAsync(d => d.FcmToken == request.FcmToken);

        if (existingDevice != null)
        {
            if (existingDevice.UserId != userId)
            {
                // Token reassigned to a different user
                existingDevice.UserId = userId;
            }
            existingDevice.Platform = request.Platform ?? existingDevice.Platform;
            existingDevice.DeviceName = request.DeviceName ?? existingDevice.DeviceName;
            existingDevice.LastSeenAt = DateTime.UtcNow;
            
            _context.Devices.Update(existingDevice);
        }
        else
        {
            // Insert new device
            var newDevice = new Device
            {
                Id = Guid.NewGuid(),
                UserId = userId,
                FcmToken = request.FcmToken,
                Platform = request.Platform,
                DeviceName = request.DeviceName,
                LastSeenAt = DateTime.UtcNow,
                CreatedAt = DateTime.UtcNow
            };
            _context.Devices.Add(newDevice);
        }

        await _context.SaveChangesAsync();
    }
}
