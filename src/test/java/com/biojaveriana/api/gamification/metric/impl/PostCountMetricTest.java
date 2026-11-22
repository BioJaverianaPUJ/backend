package com.biojaveriana.api.gamification.metric.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.post.PostRepository;

@ExtendWith(MockitoExtension.class)
class PostCountMetricTest {

    @Mock
    private PostRepository postRepository;

    private PostCountMetric metric;
    private UUID userId;
    private Instant since;

    @BeforeEach
    void setUp() {
        metric = new PostCountMetric(postRepository);
        userId = UUID.randomUUID();
        since = Instant.now();
    }

    @Test
    @DisplayName("type: reports the posts metric type")
    void type() {
        assertEquals("posts", metric.type());
    }

    @Test
    @DisplayName("value: counts all posts of the user")
    void value() {
        when(postRepository.countByAuthorId(userId)).thenReturn(7L);

        assertEquals(7L, metric.value(userId));
    }

    @Test
    @DisplayName("valueSince: counts only posts created since the given instant")
    void valueSince() {
        when(postRepository.countByAuthorIdAndCreatedAtGreaterThanEqual(userId, since)).thenReturn(3L);

        assertEquals(3L, metric.valueSince(userId, since));
    }
}
