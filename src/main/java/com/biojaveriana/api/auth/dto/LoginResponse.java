package com.biojaveriana.api.auth.dto;

import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.auth.Session;
import com.biojaveriana.api.user.Role;

public record LoginResponse(
    UUID userId,
    String email,
    Role role,
    String token,
    Instant expiresIn
) {
    public static LoginResponse from(Session session) {
        var token = session.token();
        var user = session.user();

        return new LoginResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            token.value(),
            token.expiresIn()
        );
    }
}
