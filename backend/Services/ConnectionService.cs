using Microsoft.EntityFrameworkCore;
using Moment.Api.Data;
using Moment.Api.DTOs;
using Moment.Api.Models;

namespace Moment.Api.Services;

public interface IConnectionService
{
    Task<InviteDto> CreateInviteAsync(Guid userId);
    Task<UserDto?> GetUserByInviteCodeAsync(string inviteCode);
    Task<ConnectionDto?> RequestConnectionAsync(Guid senderId, Guid targetUserId);
    Task<bool> RespondToRequestAsync(Guid receiverId, Guid connectionId, bool accept);
    Task<IEnumerable<ConnectionDto>> GetConnectionsAsync(Guid userId);
    Task<bool> RevokeConnectionAsync(Guid userId, Guid connectionId);
}

public class ConnectionService : IConnectionService
{
    private readonly MomentDbContext _context;
    private readonly IConfiguration _configuration;

    public ConnectionService(MomentDbContext context, IConfiguration configuration)
    {
        _context = context;
        _configuration = configuration;
    }

    public async Task<InviteDto> CreateInviteAsync(Guid userId)
    {
        var inviteCode = Guid.NewGuid().ToString("N")[..8].ToUpper();
        var invite = new Invite
        {
            Id = Guid.NewGuid(),
            InviteCode = inviteCode,
            SenderUserId = userId,
            CreatedAt = DateTime.UtcNow,
            ExpiresAt = DateTime.UtcNow.AddDays(7)
        };

        _context.Invites.Add(invite);
        await _context.SaveChangesAsync();

        var domain = _configuration["App:Domain"] ?? "momentapp.in";
        var inviteUrl = $"https://{domain}/invite/{inviteCode}";

        return new InviteDto(inviteCode, inviteUrl, invite.ExpiresAt);
    }

    public async Task<UserDto?> GetUserByInviteCodeAsync(string inviteCode)
    {
        var invite = await _context.Invites
            .Include(i => i.Sender)
            .FirstOrDefaultAsync(i => i.InviteCode == inviteCode && !i.IsUsed && i.ExpiresAt > DateTime.UtcNow);

        if (invite?.Sender == null) return null;

        return MapToUserDto(invite.Sender);
    }

    public async Task<ConnectionDto?> RequestConnectionAsync(Guid senderId, Guid targetUserId)
    {
        if (senderId == targetUserId) return null;

        var existing = await _context.Connections
            .FirstOrDefaultAsync(c => (c.SenderUserId == senderId && c.ReceiverUserId == targetUserId) ||
                                      (c.SenderUserId == targetUserId && c.ReceiverUserId == senderId));

        if (existing != null) return null;

        var connection = new Connection
        {
            Id = Guid.NewGuid(),
            SenderUserId = senderId,
            ReceiverUserId = targetUserId,
            Status = ConnectionStatus.PENDING,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.Connections.Add(connection);
        await _context.SaveChangesAsync();

        var targetUser = await _context.Users.FindAsync(targetUserId);
        return new ConnectionDto(connection.Id, MapToUserDto(targetUser!), connection.Status, true, connection.CreatedAt);
    }

    public async Task<bool> RespondToRequestAsync(Guid receiverId, Guid connectionId, bool accept)
    {
        var connection = await _context.Connections.FindAsync(connectionId);
        if (connection == null || connection.ReceiverUserId != receiverId || connection.Status != ConnectionStatus.PENDING)
            return false;

        connection.Status = accept ? ConnectionStatus.ACCEPTED : ConnectionStatus.REVOKED;
        connection.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<IEnumerable<ConnectionDto>> GetConnectionsAsync(Guid userId)
    {
        var connections = await _context.Connections
            .Include(c => c.Sender)
            .Include(c => c.Receiver)
            .Where(c => c.SenderUserId == userId || c.ReceiverUserId == userId)
            .ToListAsync();

        return connections.Select(c => {
            var isRequester = c.SenderUserId == userId;
            var otherUser = isRequester ? c.Receiver! : c.Sender!;
            return new ConnectionDto(c.Id, MapToUserDto(otherUser), c.Status, isRequester, c.CreatedAt);
        });
    }

    public async Task<bool> RevokeConnectionAsync(Guid userId, Guid connectionId)
    {
        var connection = await _context.Connections.FindAsync(connectionId);
        if (connection == null || (connection.SenderUserId != userId && connection.ReceiverUserId != userId))
            return false;

        connection.Status = ConnectionStatus.REVOKED;
        connection.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return true;
    }

    private UserDto MapToUserDto(User user) => new UserDto(
        user.Id,
        user.Email,
        user.Username,
        user.DisplayName,
        user.ProfilePictureUrl,
        user.Bio
    );
}
