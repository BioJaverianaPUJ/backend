package com.biojaveriana.api.motd;

import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MotdSchedulerTest {

    @Mock
    private MotdService motdService;

    private MotdScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new MotdScheduler(motdService);
    }

    @Test
    @DisplayName("rotateMotd: ensures a selection exists for today")
    void rotateMotdEnsuresSelectionForToday() {
        scheduler.rotateMotd();

        verify(motdService).ensureSelection(LocalDate.now());
    }
}
