using Amazon.S3;
using Amazon.S3.Model;

namespace Moment.Api.Services;

public interface IStorageService
{
    string GetPresignedUploadUrl(string fileName, string contentType, long contentLength);
    string GetPublicUrl(string fileName);
    string GetPresignedDownloadUrl(string fileName, TimeSpan? expiry = null);
    Task<byte[]> GetFileHeaderBytesAsync(string fileName, int byteCount);
    Task DeleteFileAsync(string fileName);
}

public class R2StorageService : IStorageService
{
    private readonly IConfiguration _configuration;
    private readonly IAmazonS3 _s3Client;

    public R2StorageService(IConfiguration configuration)
    {
        _configuration = configuration;
        
        var accessKey = _configuration["Cloudflare:AccessKeyId"];
        var secretKey = _configuration["Cloudflare:SecretAccessKey"];
        var accountId = _configuration["Cloudflare:AccountId"];

        var config = new AmazonS3Config
        {
            ServiceURL = $"https://{accountId}.r2.cloudflarestorage.com",
        };

        _s3Client = new AmazonS3Client(accessKey, secretKey, config);
    }

    public string GetPresignedUploadUrl(string fileName, string contentType, long contentLength)
    {
        var bucketName = _configuration["Cloudflare:BucketName"] ?? "moment-assets";
        
        var request = new GetPreSignedUrlRequest
        {
            BucketName = bucketName,
            Key = fileName,
            Verb = HttpVerb.PUT,
            Expires = DateTime.UtcNow.AddMinutes(15),
            ContentType = contentType
        };
        
        request.Headers.ContentLength = contentLength;

        return _s3Client.GetPreSignedURL(request);
    }

    public string GetPublicUrl(string fileName)
    {
        var publicUrl = _configuration["Cloudflare:PublicUrl"] ?? "https://pub-moment.r2.dev";
        return $"{publicUrl.TrimEnd('/')}/{fileName}";
    }

    // GetPublicUrl above builds a permanent, unauthenticated link on Cloudflare's public-
    // bucket-access domain - once the bucket's public access is turned off (a Cloudflare
    // dashboard/API setting outside this repo), only signed requests against the real R2
    // S3-compatible endpoint can read an object. This generates one of those, so the app
    // can keep referring to objects by their existing filename while every actual read the
    // client performs goes through a link that expires instead of working forever. Default
    // expiry is long enough to comfortably cover FCM push delay + WallpaperWorker's retry/
    // backoff window and the widget's periodic refresh cadence, while still meaning a leaked
    // URL (from a log, a screenshot, a shared link) stops working within a day instead of
    // granting permanent access to what is, for this app, its most sensitive content.
    public string GetPresignedDownloadUrl(string fileName, TimeSpan? expiry = null)
    {
        var bucketName = _configuration["Cloudflare:BucketName"] ?? "moment-assets";

        var request = new GetPreSignedUrlRequest
        {
            BucketName = bucketName,
            Key = fileName,
            Verb = HttpVerb.GET,
            Expires = DateTime.UtcNow.Add(expiry ?? TimeSpan.FromHours(24))
        };

        return _s3Client.GetPreSignedURL(request);
    }

    public async Task<byte[]> GetFileHeaderBytesAsync(string fileName, int byteCount)
    {
        var bucketName = _configuration["Cloudflare:BucketName"] ?? "moment-assets";
        var request = new GetObjectRequest
        {
            BucketName = bucketName,
            Key = fileName,
            ByteRange = new ByteRange(0, byteCount - 1)
        };

        try
        {
            using var response = await _s3Client.GetObjectAsync(request);
            using var ms = new MemoryStream();
            await response.ResponseStream.CopyToAsync(ms);
            return ms.ToArray();
        }
        catch (AmazonS3Exception ex) when (ex.StatusCode == System.Net.HttpStatusCode.NotFound)
        {
            return Array.Empty<byte>();
        }
    }

    public async Task DeleteFileAsync(string fileName)
    {
        var bucketName = _configuration["Cloudflare:BucketName"] ?? "moment-assets";
        var request = new DeleteObjectRequest
        {
            BucketName = bucketName,
            Key = fileName
        };

        try
        {
            await _s3Client.DeleteObjectAsync(request);
        }
        catch (AmazonS3Exception ex) when (ex.StatusCode == System.Net.HttpStatusCode.NotFound)
        {
            // Ignore if file doesn't exist
        }
    }
}
