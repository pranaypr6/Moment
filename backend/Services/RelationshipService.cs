using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Moment.Api.Data;
using Moment.Api.DTOs;
using Moment.Api.Models;

namespace Moment.Api.Services;

public class RelationshipService : IRelationshipService
{
    private readonly MomentDbContext _context;

    public RelationshipService(MomentDbContext context)
    {
        _context = context;
    }

    private async Task<RelationshipDto> MapToDtoAsync(Relationship r, Guid callerId)
    {
        var partner = r.Partner1Id == callerId ? r.Partner2 : r.Partner1;
        var isPausedByMe = r.Partner1Id == callerId ? r.Partner1PausedAt.HasValue : r.Partner2PausedAt.HasValue;
        var isPausedByPartner = r.Partner1Id == callerId ? r.Partner2PausedAt.HasValue : r.Partner1PausedAt.HasValue;

        var totalMoments = await _context.Moments.CountAsync(m => m.RelationshipId == r.Id);
        
        var signals = await _context.PresenceSignals
            .Where(p => p.RelationshipId == r.Id)
            .GroupBy(p => p.Type)
            .Select(g => new { Type = g.Key.ToString(), Count = g.Count() })
            .ToListAsync();
        var signalsCount = signals.ToDictionary(s => s.Type, s => s.Count);

        return new RelationshipDto(
            r.Id,
            new UserDto(partner!.Id, partner.DisplayName ?? "Partner", partner.ProfilePictureUrl, partner.CurrentVibe, partner.IsPremium),
            r.SpaceName,
            r.ThemeId,
            r.CoverMomentId,
            isPausedByMe,
            isPausedByPartner,
            r.Status.ToString(),
            r.CreatedAt,
            r.PairedAt,
            totalMoments,
            signalsCount
        );
    }

    public async Task<RelationshipDto?> GetCurrentRelationshipAsync(Guid userId)
    {
        var rel = await _context.Relationships
            .AsNoTracking()
            .Include(r => r.Partner1)
            .Include(r => r.Partner2)
            .Where(r => (r.Partner1Id == userId || r.Partner2Id == userId) && r.Status != RelationshipStatus.Unpaired)
            .OrderByDescending(r => r.CreatedAt)
            .FirstOrDefaultAsync();

        if (rel == null) return null;
        return await MapToDtoAsync(rel, userId);
    }

    public async Task<CreatePairingKeyResponse> CreatePairingKeyAsync(Guid userId)
    {
        for (int i = 0; i < 5; i++)
        {
            var code = Guid.NewGuid().ToString().Substring(0, 8).ToUpper();
            if (!await _context.Invites.AnyAsync(inv => inv.InviteCode == code))
            {
                var invite = new Invite
                {
                    InviteCode = code,
                    SenderUserId = userId
                };
                _context.Invites.Add(invite);
                await _context.SaveChangesAsync();
                return new CreatePairingKeyResponse(code, invite.ExpiresAt);
            }
        }
        throw new InvalidOperationException("Failed to generate a unique pairing key.");
    }

    public async Task<RelationshipDto> JoinRelationshipAsync(Guid userId, string pairingKey)
    {
        var invite = await _context.Invites
            .FirstOrDefaultAsync(i => i.InviteCode == pairingKey && !i.IsUsed && i.ExpiresAt > DateTime.UtcNow);
        
        if (invite == null) throw new InvalidOperationException("Invalid or expired pairing key.");
        if (invite.SenderUserId == userId) throw new InvalidOperationException("Cannot pair with yourself.");

        // Mark used
        invite.IsUsed = true;

        var partner1Id = userId < invite.SenderUserId ? userId : invite.SenderUserId;
        var partner2Id = userId > invite.SenderUserId ? userId : invite.SenderUserId;

        var p1 = await _context.Users.FindAsync(partner1Id);
        var p2 = await _context.Users.FindAsync(partner2Id);
        var defaultSpaceName = $"{p1?.DisplayName ?? "Partner 1"} 💞 {p2?.DisplayName ?? "Partner 2"}";

        var existingRel = await _context.Relationships
            .FirstOrDefaultAsync(r => r.Partner1Id == partner1Id && r.Partner2Id == partner2Id);

        var someoneElseRel = await _context.Relationships
            .FirstOrDefaultAsync(r => 
                (r.Partner1Id == userId || r.Partner2Id == userId || r.Partner1Id == invite.SenderUserId || r.Partner2Id == invite.SenderUserId) 
                && r.Status == RelationshipStatus.Active 
                && (r.Partner1Id != partner1Id || r.Partner2Id != partner2Id));

        if (someoneElseRel != null)
        {
            throw new InvalidOperationException("One of the users is already in an active relationship with someone else.");
        }

        Relationship rel;
        if (existingRel != null)
        {
            if (existingRel.Status == RelationshipStatus.Unpaired)
            {
                // Re-activate
                existingRel.Status = RelationshipStatus.Active;
                existingRel.PairedAt = DateTime.UtcNow;
                existingRel.CreatedByUserId = invite.SenderUserId;
                existingRel.SpaceName = defaultSpaceName;
                rel = existingRel;
            }
            else
            {
                throw new InvalidOperationException("You are already paired with this user.");
            }
        }
        else
        {
            rel = new Relationship
            {
                Partner1Id = partner1Id,
                Partner2Id = partner2Id,
                CreatedByUserId = invite.SenderUserId,
                Status = RelationshipStatus.Active,
                PairedAt = DateTime.UtcNow,
                SpaceName = defaultSpaceName
            };
            _context.Relationships.Add(rel);
        }

        await _context.SaveChangesAsync();

        // Load partners for mapping
        rel = await _context.Relationships
            .Include(r => r.Partner1)
            .Include(r => r.Partner2)
            .FirstAsync(r => r.Id == rel.Id);

        return await MapToDtoAsync(rel, userId);
    }

    public async Task<RelationshipDto> UpdateSpaceNameAsync(Guid userId, string spaceName)
    {
        var rel = await _context.Relationships
            .Include(r => r.Partner1)
            .Include(r => r.Partner2)
            .FirstOrDefaultAsync(r => (r.Partner1Id == userId || r.Partner2Id == userId) && r.Status == RelationshipStatus.Active);
        
        if (rel == null) throw new InvalidOperationException("No active relationship found.");

        rel.SpaceName = spaceName;
        await _context.SaveChangesAsync();
        return await MapToDtoAsync(rel, userId);
    }

    public async Task<RelationshipDto> UpdateThemeAsync(Guid userId, string themeId)
    {
        var rel = await _context.Relationships
            .Include(r => r.Partner1)
            .Include(r => r.Partner2)
            .FirstOrDefaultAsync(r => (r.Partner1Id == userId || r.Partner2Id == userId) && r.Status == RelationshipStatus.Active);
        
        if (rel == null) throw new InvalidOperationException("No active relationship found.");

        rel.ThemeId = themeId;
        await _context.SaveChangesAsync();
        return await MapToDtoAsync(rel, userId);
    }

    public async Task<RelationshipDto> UpdateCoverAsync(Guid userId, Guid coverMomentId)
    {
        var rel = await _context.Relationships
            .Include(r => r.Partner1)
            .Include(r => r.Partner2)
            .FirstOrDefaultAsync(r => (r.Partner1Id == userId || r.Partner2Id == userId) && r.Status == RelationshipStatus.Active);
        
        if (rel == null) throw new InvalidOperationException("No active relationship found.");

        var momentBelongsToRel = await _context.Moments.AnyAsync(m => m.Id == coverMomentId && m.RelationshipId == rel.Id);
        if (!momentBelongsToRel) throw new InvalidOperationException("Moment not found or does not belong to this relationship.");

        rel.CoverMomentId = coverMomentId;
        await _context.SaveChangesAsync();
        return await MapToDtoAsync(rel, userId);
    }

    public async Task<RelationshipDto> TogglePauseAsync(Guid userId)
    {
        var rel = await _context.Relationships
            .Include(r => r.Partner1)
            .Include(r => r.Partner2)
            .FirstOrDefaultAsync(r => (r.Partner1Id == userId || r.Partner2Id == userId) && r.Status == RelationshipStatus.Active);
        
        if (rel == null) throw new InvalidOperationException("No active relationship found.");

        if (rel.Partner1Id == userId)
        {
            rel.Partner1PausedAt = rel.Partner1PausedAt.HasValue ? null : DateTime.UtcNow;
        }
        else
        {
            rel.Partner2PausedAt = rel.Partner2PausedAt.HasValue ? null : DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();
        return await MapToDtoAsync(rel, userId);
    }

    public async Task UnpairAsync(Guid userId)
    {
        var rel = await _context.Relationships
            .FirstOrDefaultAsync(r => (r.Partner1Id == userId || r.Partner2Id == userId) && r.Status == RelationshipStatus.Active);
        
        if (rel == null) return;

        rel.Status = RelationshipStatus.Unpaired;
        rel.UnpairedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();
    }
}
