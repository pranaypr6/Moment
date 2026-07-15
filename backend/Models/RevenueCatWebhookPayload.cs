using System.Text.Json.Serialization;

namespace Moment.Api.Models;

public class RevenueCatWebhookPayload
{
    [JsonPropertyName("event")]
    public RevenueCatEvent Event { get; set; } = null!;
}

public class RevenueCatEvent
{
    [JsonPropertyName("type")]
    public string Type { get; set; } = string.Empty;

    [JsonPropertyName("app_user_id")]
    public string AppUserId { get; set; } = string.Empty;

    [JsonPropertyName("entitlement_ids")]
    public List<string> EntitlementIds { get; set; } = new();

    [JsonPropertyName("product_id")]
    public string ProductId { get; set; } = string.Empty;
}
