package dev.sergio.pulsewatch.monitor.application;

import dev.sergio.pulsewatch.monitor.domain.MonitorStatus;
import dev.sergio.pulsewatch.monitor.persistence.MonitorCheckEntity;
import java.time.Instant;
import java.util.UUID;

public record MonitorCheckView(
        Long id,
        UUID monitorId,
        String endpoint,
        MonitorStatus status,
        Integer httpStatus,
        long responseTimeMs,
        Instant checkedAt) {

    public static MonitorCheckView from(MonitorCheckEntity check, UUID monitorId) {
        return new MonitorCheckView(
                check.getId(),
                monitorId,
                check.getEndpoint(),
                check.getStatus(),
                check.getHttpStatus(),
                check.getResponseTimeMs(),
                check.getCheckedAt());
    }
}
