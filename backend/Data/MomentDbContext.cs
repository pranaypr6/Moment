using Microsoft.EntityFrameworkCore;
using Moment.Api.Models;

namespace Moment.Api.Data;

public class MomentDbContext : DbContext
{
    public MomentDbContext(DbContextOptions<MomentDbContext> options) : base(options)
    {
    }

    public DbSet<User> Users => Set<User>();
    public DbSet<Connection> Connections => Set<Connection>();
    public DbSet<Invite> Invites => Set<Invite>();
    public DbSet<Device> Devices => Set<Device>();
    public DbSet<WallpaperMoment> Moments => Set<WallpaperMoment>();
    public DbSet<Report> Reports => Set<Report>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<User>(entity =>
        {
            entity.HasIndex(e => e.FirebaseUid).IsUnique();
            entity.HasIndex(e => e.Email).IsUnique();
            entity.HasIndex(e => e.Username).IsUnique();
        });

        modelBuilder.Entity<Connection>(entity =>
        {
            entity.HasIndex(e => new { e.SenderUserId, e.ReceiverUserId }).IsUnique();
            
            entity.HasOne(e => e.Sender)
                .WithMany()
                .HasForeignKey(e => e.SenderUserId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasOne(e => e.Receiver)
                .WithMany()
                .HasForeignKey(e => e.ReceiverUserId)
                .OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<Invite>(entity =>
        {
            entity.HasIndex(e => e.InviteCode).IsUnique();
        });

        modelBuilder.Entity<Device>(entity =>
        {
            entity.HasIndex(e => e.FcmToken).IsUnique();
        });

        modelBuilder.Entity<WallpaperMoment>(entity =>
        {
            entity.HasIndex(e => e.SenderUserId);
            entity.HasIndex(e => e.ReceiverUserId);
            entity.HasIndex(e => e.CreatedAt);
            entity.HasIndex(e => e.Status);
        });
    }
}
