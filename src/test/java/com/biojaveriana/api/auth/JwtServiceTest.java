package com.biojaveriana.api.auth;

import com.biojaveriana.api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @InjectMocks
    private JwtService jwtService;

    @Test
    @DisplayName("generateToken - Should generate token successfully with correct claims and expiration")
    void generateToken_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder()
                .id(userId)
                .email("test@javeriana.edu.co")
                .build();

        String expectedTokenValue = "mock.jwt.encoded.value";
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(3600);

        // Simulamos el objeto Jwt que devuelve el encoder de Spring Security
        Jwt mockJwt = Jwt.withTokenValue(expectedTokenValue)
                .header("alg", "HS256")
                .claim("sub", userId.toString())
                .issuedAt(now)
                .expiresAt(expiry)
                .build();

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

     
        Token tokenResult = jwtService.generateToken(mockUser);

        assertThat(tokenResult).isNotNull();
        assertThat(tokenResult.value()).isEqualTo(expectedTokenValue);
        assertThat(tokenResult.expiresIn()).isCloseTo(expiry, within(1, ChronoUnit.SECONDS));
    }
}