package com.biojaveriana.api.user;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;

import com.biojaveriana.api.s3.exceptions.UnsupportedContentType;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UserResource {
    PROFILE_PICTURE("picture"),
    BANNER("banner");

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
        "image/jpeg", "jpg",
        "image/png", "png",
        "image/webp", "webp"
    );

    private static final String RESOURCE_PREFIX = "users";

    private final String path;

    public String generateKey(User user, String contentType) {
        if (!ALLOWED_TYPES.containsKey(contentType)) {
            throw new UnsupportedContentType(contentType);
        }

        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[3];
        secureRandom.nextBytes(randomBytes);

        String objectSecret = HexFormat.of().formatHex(randomBytes);
        return String.format("%s/%s/%s/%s.%s", RESOURCE_PREFIX, user.getId(), path, objectSecret, ALLOWED_TYPES);
    }
}
