package com.biojaveriana.api.motd;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MotdScheduler {
    private final MotdService motdService;

    @Scheduled(cron = "${app.motd.rotation-cron:0 0 0 * * *}")
    public void rotateMotd() {
        log.info("MOTD daily rotation triggered");
        motdService.ensureSelection(LocalDate.now());
    }
}
