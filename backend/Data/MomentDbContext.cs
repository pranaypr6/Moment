using Microsoft.EntityFrameworkCore;
using Moment.Api.Models;

namespace Moment.Api.Data;

public class MomentDbContext : DbContext
{
    public MomentDbContext(DbContextOptions<MomentDbContext> options) : base(options)
    {
    }

    public DbSet<User> Users => Set<User>();
    public DbSet<Relationship> Relationships => Set<Relationship>();
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

        modelBuilder.Entity<Relationship>(entity =>
        {
            entity.HasIndex(e => new { e.Partner1Id, e.Partner2Id }).IsUnique();

            entity.HasOne(e => e.Partner1)
                .WithMany()
                .HasForeignKey(e => e.Partner1Id)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasOne(e => e.Partner2)
                .WithMany()
                .HasForeignKey(e => e.Partner2Id)
                .OnDelete(DeleteBehavior.Restrict);
                
            entity.HasOne(e => e.CoverMoment)
                .WithMany()
                .HasForeignKey(e => e.CoverMomentId)
                .OnDelete(DeleteBehavior.SetNull);
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
            entity.HasIndex(e => e.RelationshipId);
            entity.HasIndex(e => e.CreatorUserId);
            entity.HasIndex(e => e.ReceiverUserId);
            entity.HasIndex(e => e.CreatedAt);
            entity.HasIndex(e => e.Status);
        });

        modelBuilder.Entity<Report>(entity =>
        {
            entity.HasOne(e => e.Reporter)
                .WithMany()
                .HasForeignKey(e => e.ReporterUserId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.HasOne(e => e.ReportedUser)
                .WithMany()
                .HasForeignKey(e => e.ReportedUserId)
                .OnDelete(DeleteBehavior.Restrict);
        });
    }
}
