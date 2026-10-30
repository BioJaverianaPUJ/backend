package com.biojaveriana.api.s3;

import java.time.Instant;

public record UploadPresign(
    String key,
    String uploadUrl,
    String method,
    String contentType,
    Instant expiresIn
) {}
