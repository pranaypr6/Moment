using System.ComponentModel.DataAnnotations;

namespace Moment.Api.DTOs;

public class RegisterDeviceRequest
{
    [Required]
    public string FcmToken { get; set; } = string.Empty;

    public string? Platform { get; set; }
    
    public string? DeviceName { get; set; }
}
