package com.biojaveriana.api.auth;

import com.biojaveriana.api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder()
                .id(userId)
                .email("test@javeriana.edu.co")
                .build();

        String expectedTokenValue = "mock.jwt.encoded.value";
        Instant before = Instant.now();

        Jwt mockJwt = Jwt.withTokenValue(expectedTokenValue)
                .header("alg", "HS256")
                .claim("sub", userId.toString())
                .build();

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        Token tokenResult = jwtService.generateToken(mockUser);
        Instant after = Instant.now();

        assertThat(tokenResult).isNotNull();
        assertThat(tokenResult.value()).isEqualTo(expectedTokenValue);
        assertThat(tokenResult.expiresIn()).isBetween(
                before.plusSeconds(1209598),
                after.plusSeconds(1209602));

        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());

        var claims = captor.getValue().getClaims();
        assertThat(claims.getClaims().get("iss")).isEqualTo("bio-javeriana");
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuedAt()).isBetween(before, after);
        assertThat(claims.getExpiresAt()).isBetween(
                before.plusSeconds(1209598),
                after.plusSeconds(1209602));
    }
}