package dev.sergio.pulsewatch.monitor.api;

import dev.sergio.pulsewatch.monitor.application.MonitorCheckView;
import java.time.Instant;
import java.util.UUID;

public record MonitorCheckResponse(
        Long id,
        UUID monitorId,
        String endpoint,
        String status,
        Integer httpStatus,
        long responseTimeMs,
        Instant checkedAt) {

    public static MonitorCheckResponse from(MonitorCheckView check) {
        return new MonitorCheckResponse(
                check.id(),
                check.monitorId(),
                check.endpoint(),
                check.status().name(),
                check.httpStatus(),
                check.responseTimeMs(),
                check.checkedAt());
    }
}
