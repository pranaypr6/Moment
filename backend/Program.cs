using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Moment.Api.Data;
using System.Text;
using FirebaseAdmin;
using Google.Apis.Auth.OAuth2;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.RateLimiting;
using System.Threading.RateLimiting;
using System.Security.Claims;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter());
    });
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

builder.Services.AddScoped<Moment.Api.Services.IAuthService, Moment.Api.Services.AuthService>();
builder.Services.AddScoped<Moment.Api.Services.IRelationshipService, Moment.Api.Services.RelationshipService>();
builder.Services.AddScoped<Moment.Api.Services.IMomentService, Moment.Api.Services.MomentService>();
builder.Services.AddScoped<Moment.Api.Services.IDeviceService, Moment.Api.Services.DeviceService>();
builder.Services.AddScoped<Moment.Api.Services.IPushNotificationService, Moment.Api.Services.FirebasePushNotificationService>();
builder.Services.AddScoped<Moment.Api.Services.IPresenceService, Moment.Api.Services.PresenceService>();
builder.Services.AddSingleton<Moment.Api.Services.IStorageService, Moment.Api.Services.R2StorageService>();

// Database
builder.Services.AddDbContext<MomentDbContext>(options =>
    options.UseNpgsql(builder.Configuration.GetConnectionString("DefaultConnection")));

// Firebase
var firebaseCredentialsPath = builder.Configuration["Firebase:CredentialsPath"];
if (!string.IsNullOrEmpty(firebaseCredentialsPath) && File.Exists(firebaseCredentialsPath))
{
    using var stream = new FileStream(firebaseCredentialsPath, FileMode.Open, FileAccess.Read);
    var firebaseApp = FirebaseApp.Create(new AppOptions
    {
        Credential = GoogleCredential.FromStream(stream)
    });
    Console.WriteLine($"Firebase initialized for project: {firebaseApp.Options.ProjectId}");
}
else
{
    Console.WriteLine("Warning: Firebase credentials file not found. FCM will not work.");
}

// Rate Limiting

builder.Services.AddRateLimiter(options => {
    options.AddFixedWindowLimiter("JoinLimiter", opt => {
        opt.Window = TimeSpan.FromMinutes(1);
        opt.PermitLimit = 5; // Max 5 guesses per minute
        opt.QueueLimit = 0;
    });
    options.AddPolicy("EmotionalLimiter", context => {
        var userId = context.User.FindFirst(ClaimTypes.NameIdentifier)?.Value ?? 
                     context.Connection.RemoteIpAddress?.ToString() ?? "unknown";
        
        return RateLimitPartition.GetFixedWindowLimiter(userId, _ =>
            new FixedWindowRateLimiterOptions
            {
                PermitLimit = 300,
                Window = TimeSpan.FromHours(1),
                QueueProcessingOrder = QueueProcessingOrder.OldestFirst,
                QueueLimit = 0
            });
    });
    options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
});

// Authentication
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = builder.Configuration["Jwt:Issuer"],
            ValidAudience = builder.Configuration["Jwt:Audience"],
            IssuerSigningKey = new SymmetricSecurityKey(
                Encoding.UTF8.GetBytes(builder.Configuration["Jwt:Key"] ?? "default_secret_key_for_development"))
        };
    });

var app = builder.Build();

// Apply database migrations automatically on startup
using (var scope = app.Services.CreateScope())
{
    var dbContext = scope.ServiceProvider.GetRequiredService<MomentDbContext>();
    dbContext.Database.Migrate();
}

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();
app.UseRateLimiter();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();
