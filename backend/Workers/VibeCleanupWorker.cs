using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Moment.Api.Data;

namespace Moment.Api.Workers;

public class VibeCleanupWorker : BackgroundService
{
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<VibeCleanupWorker> _logger;

    public VibeCleanupWorker(IServiceProvider serviceProvider, ILogger<VibeCleanupWorker> logger)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("VibeCleanupWorker starting.");

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                using var scope = _serviceProvider.CreateScope();
                var dbContext = scope.ServiceProvider.GetRequiredService<MomentDbContext>();

                // Calculate the expiration threshold (24 hours ago)
                var threshold = DateTime.UtcNow.AddHours(-24);

                var expiredUsers = await dbContext.Users
                    .Where(u => u.CurrentVibe != null && u.VibeUpdatedAt.HasValue && u.VibeUpdatedAt.Value < threshold)
                    .ToListAsync(stoppingToken);

                if (expiredUsers.Any())
                {
                    foreach (var user in expiredUsers)
                    {
                        user.CurrentVibe = null;
                        user.VibeUpdatedAt = null;
                        user.UpdatedAt = DateTime.UtcNow;
                    }

                    await dbContext.SaveChangesAsync(stoppingToken);
                    _logger.LogInformation($"Cleared expired vibes for {expiredUsers.Count} users.");
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error occurred executing VibeCleanupWorker.");
            }

            // Wait 1 hour before running again
            await Task.Delay(TimeSpan.FromHours(1), stoppingToken);
        }
    }
}
