package com.biojaveriana.api.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class ObjectServiceTest {

    private static final String BUCKET = "bio-javeriana-media";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner presigner;

    private ObjectService objectService;

    @BeforeEach
    void setUp() {
        objectService = new ObjectService(s3Client, presigner);
        setField(objectService, "bucket", BUCKET);
        setField(objectService, "publicEndpoint", "https://cdn.example.com");
    }

    @Test
    @DisplayName("createPresign: returns a 5 minute PUT presign for the key and content type")
    void createPresignReturnsPutPresign() {
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class)))
            .thenReturn(PresignedPutObjectRequest.builder()
                .httpRequest(SdkHttpFullRequest.builder().method(SdkHttpMethod.PUT).uri(URI.create("https://s3.example.com/signed")).build())
                .expiration(Instant.now().plusSeconds(300))
                .isBrowserExecutable(true)
                .signedHeaders(java.util.Map.of("host", java.util.List.of("s3.example.com")))
                .build());

        Instant before = Instant.now();
        UploadPresign presign = objectService.createPresign("posts/1.jpg", "image/jpeg");
        Instant after = Instant.now();

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(presigner).presignPutObject(captor.capture());
        assertEquals(BUCKET, captor.getValue().putObjectRequest().bucket());
        assertEquals("posts/1.jpg", captor.getValue().putObjectRequest().key());
        assertEquals("image/jpeg", captor.getValue().putObjectRequest().contentType());

        assertEquals("posts/1.jpg", presign.key());
        assertEquals("https://s3.example.com/signed", presign.url());
        assertEquals("PUT", presign.method());
        assertEquals("image/jpeg", presign.contentType());
        assertTrue(!presign.expiresAt().isBefore(before.plus(Duration.ofMinutes(4))));
        assertTrue(!presign.expiresAt().isAfter(after.plus(Duration.ofMinutes(6))));
    }

    @Test
    @DisplayName("confirmUpload: true when the object exists in the bucket")
    void confirmUploadTrueWhenObjectExists() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        assertTrue(objectService.confirmUpload("posts/1.jpg"));
    }

    @Test
    @DisplayName("confirmUpload: false when the key does not exist")
    void confirmUploadFalseWhenKeyMissing() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(S3Exception.builder()
            .statusCode(404)
            .awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchKey").build())
            .build());

        assertFalse(objectService.confirmUpload("missing.jpg"));
    }

    @Test
    @DisplayName("confirmUpload: other S3 errors are rethrown")
    void confirmUploadRethrowsOtherErrors() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(S3Exception.builder()
            .statusCode(403)
            .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").build())
            .build());

        assertThrows(S3Exception.class, () -> objectService.confirmUpload("posts/1.jpg"));
    }


    @Test
    @DisplayName("delete: deletes the object from the configured bucket")
    void deleteDelegatesToS3() {
        objectService.delete("posts/1.jpg");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals("posts/1.jpg", captor.getValue().key());
    }

    @Test
    @DisplayName("upload: puts the stream with the given key and content type")
    void uploadDelegatesToS3() {
        objectService.upload("posts/1.jpg", new ByteArrayInputStream(new byte[]{1}), 1, "image/jpeg");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals("posts/1.jpg", captor.getValue().key());
        assertEquals("image/jpeg", captor.getValue().contentType());
    }

    @Test
    @DisplayName("generateUrl: null or empty keys produce no URL")
    void generateUrlEmptyKeyReturnsNull() {
        assertNull(objectService.generateUrl(null));
        assertNull(objectService.generateUrl(""));
    }

    @Test
    @DisplayName("generateUrl: builds endpoint/bucket/key without double slashes")
    void generateUrlJoinsEndpointBucketKey() {
        assertEquals("https://cdn.example.com/" + BUCKET + "/posts/1.jpg", objectService.generateUrl("posts/1.jpg"));
    }

    @Test
    @DisplayName("generateUrl: a trailing slash on the endpoint is tolerated")
    void generateUrlTrailingSlashEndpoint() {
        setField(objectService, "publicEndpoint", "https://cdn.example.com/");

        assertEquals("https://cdn.example.com/" + BUCKET + "/posts/1.jpg", objectService.generateUrl("posts/1.jpg"));
    }
}
