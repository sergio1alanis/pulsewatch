package dev.sergio.pulsewatch.monitor.application;

import dev.sergio.pulsewatch.monitor.persistence.MonitorIncidentEntity;
import java.time.Instant;
import java.util.UUID;

public record MonitorIncidentView(
        UUID id,
        UUID monitorId,
        String status,
        Instant startedAt,
        Instant lastObservedAt,
        Instant resolvedAt) {

    public static MonitorIncidentView from(MonitorIncidentEntity incident) {
        return new MonitorIncidentView(
                incident.getId(),
                incident.getMonitor().getId(),
                incident.getResolvedAt() == null ? "OPEN" : "RESOLVED",
                incident.getStartedAt(),
                incident.getLastObservedAt(),
                incident.getResolvedAt());
    }
}
