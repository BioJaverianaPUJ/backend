package com.biojaveriana.api.common.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    int status,
    String code,
    String message,
    String path,
    Instant timestamp,
    Map<String, Object> details
) {}
