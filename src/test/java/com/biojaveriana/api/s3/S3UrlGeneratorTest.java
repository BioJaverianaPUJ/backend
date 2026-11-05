package com.biojaveriana.api.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

class S3UrlGeneratorTest {

    @Test
    void objectService_implementsS3UrlGenerator() {
        assertThat(S3UrlGenerator.class).isAssignableFrom(ObjectService.class);
    }

    @Test
    void generateUrl_returnsNullForNullKey() throws Exception {
        ObjectService service = serviceWithEndpoint("https://s3.example.com", "test-bucket");

        assertThat(service.generateUrl(null)).isNull();
    }

    @Test
    void generateUrl_returnsNullForEmptyKey() throws Exception {
        ObjectService service = serviceWithEndpoint("https://s3.example.com", "test-bucket");

        assertThat(service.generateUrl("")).isNull();
    }

    @Test
    void generateUrl_buildsUrlWithoutDuplicateSlash() throws Exception {
        ObjectService service = serviceWithEndpoint("https://s3.example.com/", "test-bucket");

        assertThat(service.generateUrl("specimens/photo.jpg"))
            .isEqualTo("https://s3.example.com/test-bucket/specimens/photo.jpg");
    }

    @Test
    void generateUrl_buildsUrlWhenEndpointHasNoTrailingSlash() throws Exception {
        ObjectService service = serviceWithEndpoint("https://s3.example.com", "test-bucket");

        assertThat(service.generateUrl("specimens/photo.jpg"))
            .isEqualTo("https://s3.example.com/test-bucket/specimens/photo.jpg");
    }

    private static ObjectService serviceWithEndpoint(String endpoint, String bucket) throws Exception {
        Constructor<ObjectService> constructor = ObjectService.class.getDeclaredConstructor(
            software.amazon.awssdk.services.s3.presigner.S3Presigner.class,
            software.amazon.awssdk.services.s3.S3Client.class
        );
        ObjectService service = constructor.newInstance(null, null);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "endpoint", endpoint);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "bucket", bucket);
        return service;
    }
}
