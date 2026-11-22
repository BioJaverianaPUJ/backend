package com.biojaveriana.api.motd.dto;

import java.time.Instant;
import java.time.LocalDate;

public record MotdResponse(
    LocalDate displayDate,
    Instant selectedAt,
    FunFactResponse funFact
) {
}
