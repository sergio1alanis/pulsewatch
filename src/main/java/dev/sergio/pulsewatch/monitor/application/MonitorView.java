package dev.sergio.pulsewatch.monitor.application;

import dev.sergio.pulsewatch.monitor.domain.MonitorStatus;
import dev.sergio.pulsewatch.monitor.persistence.MonitorEntity;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record MonitorView(
        UUID id,
        String name,
        URI endpoint,
        int intervalSeconds,
        boolean enabled,
        MonitorStatus status,
        Instant lastCheckedAt,
        Integer lastHttpStatus,
        Long lastResponseTimeMs) {

    public static MonitorView from(MonitorEntity monitor) {
        return new MonitorView(
                monitor.getId(),
                monitor.getName(),
                URI.create(monitor.getEndpoint()),
                monitor.getIntervalSeconds(),
                monitor.isEnabled(),
                monitor.getStatus(),
                monitor.getLastCheckedAt(),
                monitor.getLastHttpStatus(),
                monitor.getLastResponseTimeMs());
    }
}
