package com.biojaveriana.api.common.dto;

import java.time.Instant;

public record ErrorResponse(
    int status,
    String code,
    String message,
    String path,
    Instant timestamp
) {}
