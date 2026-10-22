package com.biojaveriana.api.auth;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.biojaveriana.api.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder jwtEncoder;

    public Token generateToken(User user) {
        var now = Instant.now();
        var expiry = now.plusSeconds(3600);

        var claims = JwtClaimsSet.builder()
            .issuer("bio-javeriana")
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(expiry)
            .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var params = JwtEncoderParameters.from(header, claims);

        return new Token(jwtEncoder.encode(params).getTokenValue(), expiry);
    }
}
