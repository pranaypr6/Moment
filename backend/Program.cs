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
    })
    .ConfigureApiBehaviorOptions(options =>
    {
        // By default, [ApiController] auto-validation (e.g. a missing/malformed required
        // field like PairingKey) short-circuits before the action runs and returns ASP.NET's
        // built-in ValidationProblemDetails shape ({"title":..., "errors": {...}}), which has
        // no "message" field. Every other error path in this API (see RelationshipController's
        // BadRequest(new { message = ... }) and the global exception handler below) uses a
        // {"message": "..."} shape, and the Android client only knows how to read that one -
        // otherwise it falls back to showing the raw JSON body to the user. Normalize this
        // path to the same shape so no controller can accidentally leak raw JSON to the UI.
        options.InvalidModelStateResponseFactory = context =>
        {
            var firstError = context.ModelState.Values
                .SelectMany(v => v.Errors)
                .Select(e => e.ErrorMessage)
                .FirstOrDefault();
            return new Microsoft.AspNetCore.Mvc.BadRequestObjectResult(
                new { message = firstError ?? "Invalid request." });
        };
    });
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Nothing currently calls this API from a browser: the Android client is a native app
// (Retrofit/OkHttp), which isn't subject to CORS at all - only browser-issued fetch/XHR
// requests are. The website/ folder is static privacy/terms/delete-account pages with no
// JS that calls this API. The previous origin list included momentapp.in/www.momentapp.in,
// a domain that turned out not to actually be owned by this project - no reason to trust
// an arbitrary third party's domain in the API's CORS policy for no active use case.
// If you build a real web client later (an admin panel, a companion web app, etc.), add
// its actual origin here then - an empty allowlist is the correct default until one exists.
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.WithOrigins()
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

var connectionString = Environment.GetEnvironmentVariable("DATABASE_URL") 
    ?? builder.Configuration.GetConnectionString("DefaultConnection");

if (!string.IsNullOrEmpty(connectionString) && connectionString.StartsWith("postgres"))
{
    var databaseUri = new Uri(connectionString);
    var userInfo = databaseUri.UserInfo.Split(':');
    connectionString = $"Host={databaseUri.Host};Port={databaseUri.Port};Database={databaseUri.LocalPath.TrimStart('/')};Username={userInfo[0]};Password={userInfo[1]};Ssl Mode=Prefer;Trust Server Certificate=true;";
}

builder.Services.AddDbContext<MomentDbContext>(options =>
    options.UseNpgsql(connectionString)
    // Migrations in this project are hand-written (no dotnet/dotnet-ef CLI available in the
    // sandbox this was developed in, so `dotnet ef migrations add` can't be run to
    // auto-regenerate MomentDbContextModelSnapshot.cs byte-for-byte). EF Core's
    // PendingModelChangesWarning is a defensive check that the live model built from
    // OnModelCreating exactly matches the last migration's snapshot; a trivial metadata
    // difference between the two (not an actual schema difference - the real SQL in each
    // migration's Up() is correct and already applied) trips it and crashes the app on
    // startup. Downgrading it from an error to a log line lets the app start; the real
    // fix is to run `dotnet ef migrations add` from a machine with the SDK installed to
    // regenerate a tool-verified snapshot, at which point this can be removed.
    .ConfigureWarnings(w => w.Log(Microsoft.EntityFrameworkCore.Diagnostics.RelationalEventId.PendingModelChangesWarning)));

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
    // This used to be a single global fixed-window limiter shared by every request
    // to /join across every user - only 5 pairing-key guesses per minute for the
    // ENTIRE app combined. One user brute-forcing pairing codes (or just a bug
    // causing retries) would exhaust it and lock every other user out of joining
    // for the rest of that window. Partitioning by user (falling back to IP for
    // the rare unauthenticated edge case) keeps the same 5/minute ceiling but
    // scopes it per-caller instead of globally.
    options.AddPolicy("JoinLimiter", context => {
        var userId = context.User.FindFirst(ClaimTypes.NameIdentifier)?.Value ??
                     context.Connection.RemoteIpAddress?.ToString() ?? "unknown";

        return RateLimitPartition.GetFixedWindowLimiter(userId, _ =>
            new FixedWindowRateLimiterOptions
            {
                PermitLimit = 5, // Max 5 guesses per minute per caller
                Window = TimeSpan.FromMinutes(1),
                QueueLimit = 0
            });
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

    // Presence signals and moment creation only had hourly/daily counted limits
    // enforced deep inside the service layer (MomentLimits/PresenceService config) -
    // nothing stopped a client from firing a burst of dozens of requests in a
    // single second before that counted check ever kicked in. This partitions by
    // user (falling back to IP for unauthenticated edge cases) and caps short-term
    // bursts without touching the existing hourly/daily business limits.
    options.AddPolicy("BurstLimiter", context => {
        var userId = context.User.FindFirst(ClaimTypes.NameIdentifier)?.Value ??
                     context.Connection.RemoteIpAddress?.ToString() ?? "unknown";

        return RateLimitPartition.GetFixedWindowLimiter(userId, _ =>
            new FixedWindowRateLimiterOptions
            {
                PermitLimit = 20,
                Window = TimeSpan.FromMinutes(1),
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

app.UseAuthentication();
app.UseAuthorization();

// Must run AFTER UseAuthentication/UseAuthorization: JoinLimiter/EmotionalLimiter/
// BurstLimiter all partition by context.User.FindFirst(ClaimTypes.NameIdentifier), falling
// back to the caller's IP only when that's absent. With UseRateLimiter() registered before
// UseAuthentication(), HttpContext.User was still the default unauthenticated principal at
// the point the rate limiter's partition-key selectors ran - even for a request carrying a
// perfectly valid JWT - so every "per-user" policy was silently always falling through to
// the IP branch. That's a real problem on any shared/carrier-NAT IP (very common on mobile
// networks), where one user's activity could exhaust the bucket for everyone behind the
// same IP. AuthLimiter (used on the unauthenticated login/refresh endpoints) is IP-only by
// design and unaffected by this reordering.
app.UseRateLimiter();

app.MapControllers();

app.Run();
