using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.HttpOverrides;
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

builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.WithOrigins("http://localhost:3000", "http://localhost:5173", "https://momentapp.in", "https://www.momentapp.in")
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

builder.Services.Configure<ForwardedHeadersOptions>(options =>
{
    options.ForwardedHeaders = ForwardedHeaders.XForwardedFor | ForwardedHeaders.XForwardedProto;

    // Deployed behind a PaaS edge proxy (Railway/Render) whose IP is not fixed/known,
    // so ASP.NET Core's default "only trust loopback" KnownProxies list would silently
    // discard X-Forwarded-For and every request would appear to come from the proxy's
    // own IP - collapsing IP-based rate limiting (AuthLimiter) into one shared bucket
    // for all users. Clearing KnownNetworks/KnownProxies trusts the immediate hop, which
    // is safe here because the platform's edge is the only thing that can reach this
    // process directly (no public ingress bypassing it). If you move to infra where you
    // know the exact proxy CIDR, prefer adding it to KnownProxies/KnownNetworks instead
    // of clearing them.
    options.KnownNetworks.Clear();
    options.KnownProxies.Clear();
});

builder.Services.AddScoped<Moment.Api.Services.IAuthService, Moment.Api.Services.AuthService>();
builder.Services.AddScoped<Moment.Api.Services.IRelationshipService, Moment.Api.Services.RelationshipService>();
builder.Services.AddScoped<Moment.Api.Services.IMomentService, Moment.Api.Services.MomentService>();
builder.Services.AddScoped<Moment.Api.Services.IDeviceService, Moment.Api.Services.DeviceService>();
builder.Services.AddScoped<Moment.Api.Services.IPushNotificationService, Moment.Api.Services.FirebasePushNotificationService>();
builder.Services.AddScoped<Moment.Api.Services.IPresenceService, Moment.Api.Services.PresenceService>();
builder.Services.AddScoped<Moment.Api.Services.IReportService, Moment.Api.Services.ReportService>();
builder.Services.AddSingleton<Moment.Api.Services.IStorageService, Moment.Api.Services.R2StorageService>();
builder.Services.AddHostedService<Moment.Api.Workers.VibeCleanupWorker>();

// Database
builder.Services.AddDbContext<MomentDbContext>(options =>
    options.UseNpgsql(builder.Configuration.GetConnectionString("DefaultConnection")));

// Firebase
var firebaseCredentialsBase64 = builder.Configuration["FIREBASE_CREDENTIALS_BASE64"] 
                                ?? Environment.GetEnvironmentVariable("FIREBASE_CREDENTIALS_BASE64");

var startupLogger = LoggerFactory.Create(cfg => cfg.AddConsole()).CreateLogger("Startup");
if (!string.IsNullOrEmpty(firebaseCredentialsBase64))
{
    var decodedCredentials = System.Text.Encoding.UTF8.GetString(Convert.FromBase64String(firebaseCredentialsBase64));
    var firebaseApp = FirebaseApp.Create(new AppOptions
    {
        Credential = GoogleCredential.FromJson(decodedCredentials)
    });
    startupLogger.LogInformation("Firebase initialized for project: {ProjectId}", firebaseApp.Options.ProjectId);
}
else
{
    startupLogger.LogWarning("FIREBASE_CREDENTIALS_BASE64 env var or setting not found. FCM will not work.");
}

// Rate Limiting

builder.Services.AddRateLimiter(options => {
    options.AddFixedWindowLimiter("JoinLimiter", opt => {
        opt.Window = TimeSpan.FromMinutes(1);
        opt.PermitLimit = 5; // Max 5 guesses per minute
        opt.QueueLimit = 0;
    });
    
    options.AddPolicy("AuthLimiter", context => {
        var ip = context.Connection.RemoteIpAddress?.ToString() ?? "unknown";
        return RateLimitPartition.GetFixedWindowLimiter(ip, _ =>
            new FixedWindowRateLimiterOptions
            {
                PermitLimit = 10,
                Window = TimeSpan.FromMinutes(1),
                QueueLimit = 0
            });
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
                    Encoding.UTF8.GetBytes(builder.Configuration["Jwt:Key"] ?? throw new InvalidOperationException("JWT Secret is missing in configuration.")))
        };
    });

var app = builder.Build();

// Migrations are intentionally NOT applied on startup. Auto-migrating on every boot is
// unsafe with multiple concurrent instances (two processes can race to apply the same
// migration, or a bad migration crash-loops every instance on every deploy with no lock/
// leader election). Instead, run `dotnet ef database update` as an explicit, separate
// step in your deploy pipeline BEFORE the new app version starts serving traffic - see
// backend/DEPLOY.md for the documented process.


// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseForwardedHeaders();
app.UseHttpsRedirection();

// Global Exception Handler
app.UseExceptionHandler(errorApp =>
{
    errorApp.Run(async context =>
    {
        context.Response.StatusCode = 500;
        context.Response.ContentType = "text/plain";
        await context.Response.WriteAsync("An internal server error occurred.");
    });
});

app.UseCors();

app.UseRateLimiter();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();
