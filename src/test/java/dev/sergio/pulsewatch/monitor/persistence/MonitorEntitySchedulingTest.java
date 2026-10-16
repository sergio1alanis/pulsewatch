package dev.sergio.pulsewatch.monitor.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sergio.pulsewatch.monitor.check.ProbeResult;
import dev.sergio.pulsewatch.monitor.domain.MonitorStatus;
import dev.sergio.pulsewatch.monitor.domain.MonitorTarget;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MonitorEntitySchedulingTest {

    @Test
    void isDueOnCreationThenOnlyAfterItsIntervalAndNeverWhileDisabled() {
        Instant checkedAt = Instant.parse("2026-01-01T00:00:00Z");
        MonitorEntity monitor = new MonitorEntity(
                "Payments API",
                MonitorTarget.create("https://example.com/health", 60));

        assertTrue(monitor.isDueAt(checkedAt));

        monitor.recordProbe(new ProbeResult(MonitorStatus.UP, 200, 12, checkedAt));
        assertFalse(monitor.isDueAt(checkedAt.plusSeconds(59)));
        assertTrue(monitor.isDueAt(checkedAt.plusSeconds(60)));

        monitor.setEnabled(false);
        assertFalse(monitor.isDueAt(checkedAt.plusSeconds(61)));
    }
}
