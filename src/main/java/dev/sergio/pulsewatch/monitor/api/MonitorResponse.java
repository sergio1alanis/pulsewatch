package dev.sergio.pulsewatch.monitor.api;

import dev.sergio.pulsewatch.monitor.application.MonitorView;
import java.time.Instant;
import java.util.UUID;

public record MonitorResponse(
        UUID id,
        String name,
        String endpoint,
        int intervalSeconds,
        boolean enabled,
        String status,
        Instant lastCheckedAt,
        Integer lastHttpStatus,
        Long lastResponseTimeMs) {

    public static MonitorResponse from(MonitorView monitor) {
        return new MonitorResponse(
                monitor.id(),
                monitor.name(),
                monitor.endpoint().toASCIIString(),
                monitor.intervalSeconds(),
                monitor.enabled(),
                monitor.status().name(),
                monitor.lastCheckedAt(),
                monitor.lastHttpStatus(),
                monitor.lastResponseTimeMs());
    }
}
