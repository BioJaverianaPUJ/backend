package com.biojaveriana.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordSecurityTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Test
    void bCryptEncoderUsesAdaptiveHashWithSalting() {
        String rawPassword = "StrongPassword123!";

        String firstHash = encoder.encode(rawPassword);
        String secondHash = encoder.encode(rawPassword);

        assertThat(firstHash).startsWith("$2");
        assertThat(firstHash).isNotEqualTo(rawPassword);
        assertThat(secondHash).isNotEqualTo(rawPassword);
        assertThat(firstHash).isNotEqualTo(secondHash);
        assertThat(encoder.matches(rawPassword, firstHash)).isTrue();
        assertThat(encoder.matches(rawPassword, secondHash)).isTrue();
    }

    @Test
    void incorrectPasswordIsRejectedByBCryptEncoder() {
        String hash = encoder.encode("StrongPassword123!");

        assertThat(encoder.matches("WrongPassword123!", hash)).isFalse();
    }
}
