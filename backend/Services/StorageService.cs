using Amazon.S3;
using Amazon.S3.Model;

namespace Moment.Api.Services;

public interface IStorageService
{
    string GetPresignedUploadUrl(string fileName, string contentType);
    string GetPublicUrl(string fileName);
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

    public string GetPresignedUploadUrl(string fileName, string contentType)
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

        return _s3Client.GetPreSignedURL(request);
    }

    public string GetPublicUrl(string fileName)
    {
        var publicUrl = _configuration["Cloudflare:PublicUrl"] ?? "https://pub-moment.r2.dev";
        return $"{publicUrl.TrimEnd('/')}/{fileName}";
    }
}
