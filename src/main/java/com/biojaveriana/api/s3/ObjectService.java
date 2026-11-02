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

        return new UploadPresign(
            key,
            presigned.url().toString(),
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

        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return base + "/" + bucket + "/" + key;
    }
}
