package com.biojaveriana.api.gamification.dto;

import java.time.Instant;
import java.util.UUID;

public record XpLogResponse(
    UUID id,
    Long amount,
    String reason,
    Long totalAfter,
    Integer levelAfter,
    Instant createdAt
) {
    public static XpLogResponse from(com.biojaveriana.api.gamification.model.XpLog log) {
        return new XpLogResponse(
            log.getId(),
            log.getAmount(),
            log.getReason(),
            log.getTotalAfter(),
            log.getLevelAfter(),
            log.getCreatedAt()
        );
    }
}

