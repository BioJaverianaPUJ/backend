package com.biojaveriana.api.post.dto;

import java.util.UUID;

public record PostImageResponse(
    UUID id,
    String s3Key,
    String url,
    Integer orderIndex
) {
}
