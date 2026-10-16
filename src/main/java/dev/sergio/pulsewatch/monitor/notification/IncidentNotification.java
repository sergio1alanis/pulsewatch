package dev.sergio.pulsewatch.monitor.notification;

import dev.sergio.pulsewatch.monitor.persistence.MonitorIncidentEntity;
import dev.sergio.pulsewatch.monitor.persistence.MonitorEntity;
import java.util.UUID;

public record IncidentNotification(
        String event,
        UUID incidentId,
        UUID monitorId,
        String monitorName,
        String endpoint,
        String startedAt,
        String lastObservedAt,
        String resolvedAt) {

    public static IncidentNotification from(String event, MonitorIncidentEntity incident) {
        MonitorEntity monitor = incident.getMonitor();
        return new IncidentNotification(
                event,
                incident.getId(),
                monitor.getId(),
                monitor.getName(),
                monitor.getEndpoint(),
                incident.getStartedAt().toString(),
                incident.getLastObservedAt().toString(),
                incident.getResolvedAt() == null ? null : incident.getResolvedAt().toString());
    }
}
