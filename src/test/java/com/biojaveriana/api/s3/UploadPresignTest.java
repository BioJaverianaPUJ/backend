package com.biojaveriana.api.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class UploadPresignTest {

    @Test
    void record_exposesAllUploadDetails() {
        Instant expiresAt = Instant.parse("2026-11-03T20:00:00Z");
        UploadPresign uploadPresign = new UploadPresign(
            "specimens/photo.jpg",
            "https://s3.example.com/presigned-url",
            "PUT",
            "image/jpeg",
            expiresAt
        );

        assertThat(uploadPresign.key()).isEqualTo("specimens/photo.jpg");
        assertThat(uploadPresign.url()).isEqualTo("https://s3.example.com/presigned-url");
        assertThat(uploadPresign.method()).isEqualTo("PUT");
        assertThat(uploadPresign.contentType()).isEqualTo("image/jpeg");
        assertThat(uploadPresign.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void record_supportsValueEquality() {
        Instant expiresAt = Instant.parse("2026-11-03T20:00:00Z");
        UploadPresign first = new UploadPresign(
            "key", "url", "PUT", "image/jpeg", expiresAt
        );
        UploadPresign second = new UploadPresign(
            "key", "url", "PUT", "image/jpeg", expiresAt
        );

        assertThat(first)
            .isEqualTo(second)
            .hasSameHashCodeAs(second);
    }

    @Test
    void record_distinguishesDifferentValues() {
        Instant expiresAt = Instant.parse("2026-11-03T20:00:00Z");
        UploadPresign original = new UploadPresign(
            "key", "url", "PUT", "image/jpeg", expiresAt
        );

        assertThat(original)
            .isNotEqualTo(new UploadPresign("other-key", "url", "PUT", "image/jpeg", expiresAt))
            .isNotEqualTo(new UploadPresign("key", "other-url", "PUT", "image/jpeg", expiresAt))
            .isNotEqualTo(new UploadPresign("key", "url", "POST", "image/jpeg", expiresAt))
            .isNotEqualTo(new UploadPresign("key", "url", "PUT", "image/png", expiresAt))
            .isNotEqualTo(new UploadPresign("key", "url", "PUT", "image/jpeg", expiresAt.plusSeconds(1)));
    }
}
