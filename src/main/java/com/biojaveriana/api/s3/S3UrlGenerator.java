package com.biojaveriana.api.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class S3UrlGenerator {
    @Value("${app.s3.endpoint}")
    private String endpoint;

    @Value("${app.s3.bucket}")
    private String bucket;

    public String generateUrl(String key) {
        if (key == null) {
            return null;
        }

        String trimmedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return String.format("%s/%s/%s", trimmedEndpoint, bucket, key);
    }
}
