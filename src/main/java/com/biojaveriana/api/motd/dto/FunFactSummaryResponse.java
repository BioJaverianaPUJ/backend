package com.biojaveriana.api.motd.dto;

import java.time.Instant;
import java.util.UUID;

public record FunFactSummaryResponse(
    UUID id,
    UserSummaryResponse author,
    String title,
    String firstImageUrl,
    Instant createdAt
) {
}
