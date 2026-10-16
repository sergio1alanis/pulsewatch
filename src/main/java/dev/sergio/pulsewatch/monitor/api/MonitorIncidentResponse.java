package dev.sergio.pulsewatch.monitor.api;

import dev.sergio.pulsewatch.monitor.application.MonitorIncidentView;
import java.time.Instant;
import java.util.UUID;

public record MonitorIncidentResponse(
        UUID id,
        UUID monitorId,
        String status,
        Instant startedAt,
        Instant lastObservedAt,
        Instant resolvedAt) {

    public static MonitorIncidentResponse from(MonitorIncidentView incident) {
        return new MonitorIncidentResponse(
                incident.id(),
                incident.monitorId(),
                incident.status(),
                incident.startedAt(),
                incident.lastObservedAt(),
                incident.resolvedAt());
    }
}
