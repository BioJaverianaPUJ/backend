package com.biojaveriana.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

class JwtServiceTest {
    private static final String SECRET = "M+n7MHJvcNwm6NqYcTNVRgpuGziL/jvM+BHBC7iQ1d8=";

    private JwtService jwtService;
    private JwtDecoder jwtDecoder;
    private User user;

    @BeforeEach
    void setUp() {
        SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(SECRET), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        jwtService = new JwtService(encoder);
        jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        user = User.builder().id(UUID.randomUUID()).role(Role.USER).build();
    }

    @Test
    @DisplayName("generateToken: token carries the user id as subject and the app issuer")
    void generateTokenCarriesUserIdAndIssuer() {
        Token token = jwtService.generateToken(user);

        Jwt jwt = jwtDecoder.decode(token.value());

        assertEquals(user.getId().toString(), jwt.getSubject());
        assertEquals("bio-javeriana", jwt.getClaims().get("iss"));
    }

    @Test
    @DisplayName("generateToken: token signed with the app key is rejected by a decoder with a different key")
    void tokenSignedWithDifferentKeyFailsVerification() {
        Token token = jwtService.generateToken(user);
        SecretKey wrongKey = new SecretKeySpec(
            Base64.getDecoder().decode("YWRkYWRkYWRkYWRkYWRkYWRkYWRkYWRkYWRkYWRkYWRkYQ=="), "HmacSHA256");
        JwtDecoder wrongKeyDecoder = NimbusJwtDecoder.withSecretKey(wrongKey).macAlgorithm(MacAlgorithm.HS256).build();

        assertThrows(JwtException.class, () -> wrongKeyDecoder.decode(token.value()));
    }

    @Test
    @DisplayName("generateToken: expiry is two weeks after issuance and matches the Token record")
    void generateTokenExpiresInTwoWeeks() {
        Token token = jwtService.generateToken(user);

        Jwt jwt = jwtDecoder.decode(token.value());

        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());
        assertEquals(1209600, Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds());
        assertEquals(jwt.getExpiresAt(), token.expiresIn().truncatedTo(ChronoUnit.SECONDS));
        assertTrue(!jwt.getIssuedAt().isBefore(Instant.now().minusSeconds(5)));
    }

    @Test
    @DisplayName("generateToken: different users produce different subjects")
    void generateTokenDiffersPerUser() {
        Token first = jwtService.generateToken(user);
        Token second = jwtService.generateToken(User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build());

        assertNotEquals(jwtDecoder.decode(first.value()).getSubject(),
            jwtDecoder.decode(second.value()).getSubject());
    }
}
