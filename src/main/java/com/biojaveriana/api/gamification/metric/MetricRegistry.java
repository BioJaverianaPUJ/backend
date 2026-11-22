package com.biojaveriana.api.gamification.metric;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registry of all {@link Metric} implementations, looked up by type.
 */
@Component
public class MetricRegistry {
    private static final Logger log = LoggerFactory.getLogger(MetricRegistry.class);

    private final Map<String, Metric> metrics = new HashMap<>();

    public MetricRegistry(List<Metric> metricBeans) {
        for (Metric metric : metricBeans) {
            String type = normalize(metric.type());
            if (metrics.putIfAbsent(type, metric) != null) {
                log.warn("Duplicate metric type '{}'; keeping the first registration.", type);
            } else {
                log.debug("Registered metric type '{}'.", type);
            }
        }
    }

    public Optional<Metric> get(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(metrics.get(normalize(type)));
    }

    private String normalize(String type) {
        return type.trim().toLowerCase(Locale.ROOT);
    }
}