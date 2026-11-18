package com.biojaveriana.api.s3;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class ObjectService implements S3UrlGenerator {
    private final S3Presigner presigner;
    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.endpoint}")
    private String endpoint;

    @Value("${app.s3.public-endpoint}")
    private String publicEndpoint;

    public UploadPresign createPresign(String key, String contentType) {
        var request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build();

        var expiry = Instant.now().plus(Duration.ofMinutes(5));

        var presigned = presigner.presignPutObject(
            PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(5))
            .putObjectRequest(request)
            .build()
        );

        // Replace the internal endpoint host with the public endpoint
        String uploadUrl = replaceHost(presigned.url().toString());

        return new UploadPresign(
            key,
            uploadUrl,
            "PUT",
            contentType,
            expiry
        );
    }

    public boolean confirmUpload(String key) {
        var request = HeadObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        try {
            s3Client.headObject(request);
            return true;
        } catch (S3Exception e) {
            // 404 means the object doesn't exist → upload not confirmed
            if ("NoSuchKey".equals(e.awsErrorDetails().errorCode()) || "NotFound".equals(e.awsErrorDetails().errorCode())) {
                return false;
            }
            throw e;
        }
    }

    public void delete(String key) {
        var request = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        s3Client.deleteObject(request);
    }

    public void upload(String key, InputStream stream, long size, String contentType) {
        var request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build();

        s3Client.putObject(
            request,
            RequestBody.fromInputStream(stream, size)
        );
    }

    public String generateUrl(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        String base = publicEndpoint.endsWith("/") ? publicEndpoint.substring(0, publicEndpoint.length() - 1) : publicEndpoint;
        return base + "/" + bucket + "/" + key;
    }

    /**
     * Replace the host in a presigned URL with the public endpoint,
     * preserving the path and query parameters (signature, expiry, etc.).
     */
    private String replaceHost(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd == -1) {
            return url;
        }
        int pathStart = url.indexOf("/", schemeEnd + 3);
        if (pathStart == -1) {
            return publicEndpoint + "/";
        }
        String pathAndQuery = url.substring(pathStart);
        return publicEndpoint + pathAndQuery;
    }
}
