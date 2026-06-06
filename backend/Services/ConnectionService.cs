using Microsoft.EntityFrameworkCore;
using Moment.Api.Data;
using Moment.Api.DTOs;
using Moment.Api.Models;

namespace Moment.Api.Services;

public interface IConnectionService
{
    Task<InviteDto> CreateInviteAsync(Guid userId);
    Task<UserDto?> GetUserByInviteCodeAsync(string inviteCode);
    Task<ConnectionRequestDto?> RequestConnectionAsync(Guid senderId, Guid targetUserId);
    Task<bool> RespondToRequestAsync(Guid receiverId, Guid requestId, bool accept);
    Task<IEnumerable<ConnectionDto>> GetConnectionsAsync(Guid userId);
    Task<IEnumerable<ConnectionRequestDto>> GetPendingRequestsAsync(Guid userId);
    Task<IEnumerable<ConnectionRequestDto>> GetSentRequestsAsync(Guid userId);
    Task<bool> RevokeConnectionAsync(Guid userId, Guid targetUserId);
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

    public async Task<ConnectionRequestDto?> RequestConnectionAsync(Guid senderId, Guid targetUserId)
    {
        if (senderId == targetUserId) return null;

        // Check if already connected
        var connected = await _context.UserConnections.AnyAsync(c => c.UserId == senderId && c.ConnectedUserId == targetUserId);
        if (connected) return null;

        // Check for existing request
        var existingRequest = await _context.ConnectionRequests
            .FirstOrDefaultAsync(r => r.SenderUserId == senderId && r.ReceiverUserId == targetUserId && r.Status == RequestStatus.PENDING);

        if (existingRequest != null) return MapToRequestDto(existingRequest, existingRequest.Receiver!);

        var request = new ConnectionRequest
        {
            Id = Guid.NewGuid(),
            SenderUserId = senderId,
            ReceiverUserId = targetUserId,
            Status = RequestStatus.PENDING,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.ConnectionRequests.Add(request);
        await _context.SaveChangesAsync();

        var targetUser = await _context.Users.FindAsync(targetUserId);
        return MapToRequestDto(request, targetUser!);
    }

    public async Task<bool> RespondToRequestAsync(Guid receiverId, Guid requestId, bool accept)
    {
        var request = await _context.ConnectionRequests.FindAsync(requestId);
        if (request == null || request.ReceiverUserId != receiverId || request.Status != RequestStatus.PENDING)
            return false;

        using var transaction = await _context.Database.BeginTransactionAsync();
        try
        {
            if (accept)
            {
                request.Status = RequestStatus.ACCEPTED;
                
                // Create Dual Rows
                var edge1 = new UserConnection { UserId = request.SenderUserId, ConnectedUserId = request.ReceiverUserId, CreatedAt = DateTime.UtcNow };
                var edge2 = new UserConnection { UserId = request.ReceiverUserId, ConnectedUserId = request.SenderUserId, CreatedAt = DateTime.UtcNow };
                
                _context.UserConnections.AddRange(edge1, edge2);
            }
            else
            {
                request.Status = RequestStatus.REJECTED;
            }

            request.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
            await transaction.CommitAsync();
            return true;
        }
        catch
        {
            await transaction.RollbackAsync();
            return false;
        }
    }

    public async Task<IEnumerable<ConnectionDto>> GetConnectionsAsync(Guid userId)
    {
        var connections = await _context.UserConnections
            .Include(c => c.ConnectedUser)
            .Where(c => c.UserId == userId)
            .OrderByDescending(c => c.CreatedAt)
            .ToListAsync();

        return connections.Select(c => new ConnectionDto(
            c.ConnectedUserId, 
            MapToUserDto(c.ConnectedUser!), 
            c.Alias, 
            c.IsMuted, 
            c.IsPinned, 
            c.CreatedAt
        ));
    }

    public async Task<IEnumerable<ConnectionRequestDto>> GetPendingRequestsAsync(Guid userId)
    {
        var requests = await _context.ConnectionRequests
            .Include(r => r.Sender)
            .Where(r => r.ReceiverUserId == userId && r.Status == RequestStatus.PENDING)
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        return requests.Select(r => MapToRequestDto(r, r.Sender!));
    }

    public async Task<IEnumerable<ConnectionRequestDto>> GetSentRequestsAsync(Guid userId)
    {
        var requests = await _context.ConnectionRequests
            .Include(r => r.Receiver)
            .Where(r => r.SenderUserId == userId && r.Status == RequestStatus.PENDING)
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        return requests.Select(r => MapToRequestDto(r, r.Receiver!));
    }

    public async Task<bool> RevokeConnectionAsync(Guid userId, Guid targetUserId)
    {
        var edges = await _context.UserConnections
            .Where(c => (c.UserId == userId && c.ConnectedUserId == targetUserId) ||
                        (c.UserId == targetUserId && c.ConnectedUserId == userId))
            .ToListAsync();

        if (!edges.Any()) return false;

        _context.UserConnections.RemoveRange(edges);
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

    private ConnectionRequestDto MapToRequestDto(ConnectionRequest r, User otherUser) => new ConnectionRequestDto(
        r.Id,
        MapToUserDto(otherUser),
        r.Status,
        r.CreatedAt
    );
}
