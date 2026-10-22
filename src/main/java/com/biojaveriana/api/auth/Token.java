package com.biojaveriana.api.auth;

import java.time.Instant;

public record Token(
    String value,
    Instant expiresIn
) {}
