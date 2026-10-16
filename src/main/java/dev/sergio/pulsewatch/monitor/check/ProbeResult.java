package dev.sergio.pulsewatch.monitor.check;

import dev.sergio.pulsewatch.monitor.domain.MonitorStatus;
import java.time.Instant;

public record ProbeResult(
        MonitorStatus status,
        Integer httpStatus,
        long responseTimeMs,
        Instant checkedAt) {
}
