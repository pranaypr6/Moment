using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Moment.Api.Data;
using System.Text;
using FirebaseAdmin;
using Google.Apis.Auth.OAuth2;

using System.Text.Json.Serialization;

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

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();
