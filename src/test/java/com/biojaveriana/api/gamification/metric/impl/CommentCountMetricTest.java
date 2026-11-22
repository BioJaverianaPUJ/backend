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

import com.biojaveriana.api.comment.CommentRepository;

@ExtendWith(MockitoExtension.class)
class CommentCountMetricTest {

    @Mock
    private CommentRepository commentRepository;

    private CommentCountMetric metric;
    private UUID userId;
    private Instant since;

    @BeforeEach
    void setUp() {
        metric = new CommentCountMetric(commentRepository);
        userId = UUID.randomUUID();
        since = Instant.now();
    }

    @Test
    @DisplayName("type: reports the comments metric type")
    void type() {
        assertEquals("comments", metric.type());
    }

    @Test
    @DisplayName("value: counts all comments of the user")
    void value() {
        when(commentRepository.countByAuthorId(userId)).thenReturn(5L);

        assertEquals(5L, metric.value(userId));
    }

    @Test
    @DisplayName("valueSince: counts only comments created since the given instant")
    void valueSince() {
        when(commentRepository.countByAuthorIdAndCreatedAtGreaterThanEqual(userId, since)).thenReturn(2L);

        assertEquals(2L, metric.valueSince(userId, since));
    }
}
