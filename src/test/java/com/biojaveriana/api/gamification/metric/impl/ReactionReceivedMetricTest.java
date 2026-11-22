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

import com.biojaveriana.api.comment.CommentReactionRepository;

@ExtendWith(MockitoExtension.class)
class ReactionReceivedMetricTest {

    @Mock
    private CommentReactionRepository commentReactionRepository;

    private ReactionReceivedMetric metric;
    private UUID userId;
    private Instant since;

    @BeforeEach
    void setUp() {
        metric = new ReactionReceivedMetric(commentReactionRepository);
        userId = UUID.randomUUID();
        since = Instant.now();
    }

    @Test
    @DisplayName("type: reports the reactions_received metric type")
    void type() {
        assertEquals("reactions_received", metric.type());
    }

    @Test
    @DisplayName("value: counts all reactions received by the user")
    void value() {
        when(commentReactionRepository.countReactionsReceivedByUser(userId)).thenReturn(12L);

        assertEquals(12L, metric.value(userId));
    }

    @Test
    @DisplayName("valueSince: counts only reactions received since the given instant")
    void valueSince() {
        when(commentReactionRepository.countReactionsReceivedByUserSince(userId, since)).thenReturn(6L);

        assertEquals(6L, metric.valueSince(userId, since));
    }
}
