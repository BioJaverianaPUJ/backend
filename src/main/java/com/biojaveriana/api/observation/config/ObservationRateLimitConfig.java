package com.biojaveriana.api.observation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ObservationRateLimitConfig {
    @Value("${app.observation-rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.observation-rate-limit.cooldown-seconds:300}")
    private long cooldownSeconds;

    public boolean isEnabled() {
        return enabled;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }
}