package com.biojaveriana.api.s3;

import java.time.Instant;

public record UploadPresign(
    String key,
    String url,
    String method,
    String contentType,
    Instant expiresAt
) {}