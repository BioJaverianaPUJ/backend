package com.biojaveriana.api.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@ExtendWith(MockitoExtension.class)
class ObjectServiceTest {

    @Mock
    private S3Presigner presigner;

    @Mock
    private S3Client s3Client;

    @Mock
    private PresignedPutObjectRequest presignedPutObject;

    private ObjectService objectService;

    @BeforeEach
    void setUp() {
        objectService = new ObjectService(presigner, s3Client);
        ReflectionTestUtils.setField(objectService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(objectService, "endpoint", "https://s3.example.com/");
    }

    @Test
    void createPresign_returnsUploadDetailsAndBuildsPutRequest() throws Exception {
        URL signedUrl = new URI("https://s3.example.com/test-bucket/specimens/photo.jpg?signature=test").toURL();
        when(presignedPutObject.url()).thenReturn(signedUrl);
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedPutObject);

        Instant before = Instant.now();
        UploadPresign result = objectService.createPresign("specimens/photo.jpg", "image/jpeg");
        Instant after = Instant.now();

        assertThat(result.key()).isEqualTo("specimens/photo.jpg");
        assertThat(result.url()).isEqualTo(signedUrl.toString());
        assertThat(result.method()).isEqualTo("PUT");
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.expiresAt()).isBetween(before.plusSeconds(5 * 60 - 1), after.plusSeconds(5 * 60 + 1));
        verify(presigner).presignPutObject(argThat((PutObjectPresignRequest request) ->
            request.signatureDuration().equals(java.time.Duration.ofMinutes(5))
                && request.putObjectRequest().bucket().equals("test-bucket")
                && request.putObjectRequest().key().equals("specimens/photo.jpg")
                && request.putObjectRequest().contentType().equals("image/jpeg")
        ));
    }

    @Test
    void confirmUpload_returnsTrueWhenObjectExists() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(null);

        assertThat(objectService.confirmUpload("specimens/photo.jpg")).isTrue();

        verify(s3Client).headObject(HeadObjectRequest.builder()
            .bucket("test-bucket")
            .key("specimens/photo.jpg")
            .build());
    }

    @Test
    void confirmUpload_returnsFalseForNotFoundError() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(s3Exception("NotFound"));

        assertThat(objectService.confirmUpload("missing.jpg")).isFalse();
    }

    @Test
    void confirmUpload_returnsFalseForNoSuchKeyError() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(s3Exception("NoSuchKey"));

        assertThat(objectService.confirmUpload("missing.jpg")).isFalse();
    }

    @Test
    void confirmUpload_rethrowsUnexpectedS3Errors() {
        S3Exception exception = s3Exception("AccessDenied");
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(exception);

        assertThatThrownBy(() -> objectService.confirmUpload("protected.jpg"))
            .isSameAs(exception);
    }

    @Test
    void delete_sendsBucketAndKey() {
        objectService.delete("specimens/photo.jpg");

        verify(s3Client).deleteObject(DeleteObjectRequest.builder()
            .bucket("test-bucket")
            .key("specimens/photo.jpg")
            .build());
    }

    @Test
    void upload_sendsBucketKeyContentTypeAndBody() {
        byte[] content = "image-content".getBytes();
        ByteArrayInputStream stream = new ByteArrayInputStream(content);

        objectService.upload("specimens/photo.jpg", stream, content.length, "image/jpeg");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(requestCaptor.getValue().key()).isEqualTo("specimens/photo.jpg");
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/jpeg");
        verifyNoMoreInteractions(s3Client);
    }

    private static S3Exception s3Exception(String errorCode) {
        return (S3Exception) S3Exception.builder()
            .awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).build())
            .build();
    }
}
