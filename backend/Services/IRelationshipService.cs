namespace Moment.Api.Services;

using Moment.Api.DTOs;

public interface IRelationshipService
{
    Task<RelationshipDto?> GetCurrentRelationshipAsync(Guid userId);
    Task<CreatePairingKeyResponse> CreatePairingKeyAsync(Guid userId);
    Task<RelationshipDto> JoinRelationshipAsync(Guid userId, string pairingKey);
    Task<RelationshipDto> UpdateSpaceNameAsync(Guid userId, string spaceName);
    Task<RelationshipDto> UpdateThemeAsync(Guid userId, string themeId);
    Task<RelationshipDto> UpdateCoverAsync(Guid userId, Guid coverMomentId);
    Task<RelationshipDto> TogglePauseAsync(Guid userId);
    Task UnpairAsync(Guid userId);
}
