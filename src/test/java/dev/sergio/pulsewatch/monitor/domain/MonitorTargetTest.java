package dev.sergio.pulsewatch.monitor.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MonitorTargetTest {

    @Test
    void acceptsHttpAndHttpsEndpointsAtSupportedIntervalBounds() {
        MonitorTarget minimum = MonitorTarget.create("https://example.com/health", 30);
        MonitorTarget maximum = MonitorTarget.create("http://status.example.com", 3600);

        assertEquals(URI.create("https://example.com/health"), minimum.endpoint());
        assertEquals(Duration.ofSeconds(30), minimum.checkInterval());
        assertEquals(Duration.ofHours(1), maximum.checkInterval());
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertThrows(IllegalArgumentException.class,
                () -> MonitorTarget.create("ftp://example.com/health", 60));
    }

    @Test
    void rejectsUrlsWithoutAHost() {
        assertThrows(IllegalArgumentException.class,
                () -> MonitorTarget.create("https:///health", 60));
    }

    @Test
    void rejectsUrlsContainingCredentials() {
        assertThrows(IllegalArgumentException.class,
                () -> MonitorTarget.create("https://user:secret@example.com/health", 60));
    }

    @Test
    void rejectsIntervalsOutsideTheSafeRange() {
        assertThrows(IllegalArgumentException.class,
                () -> MonitorTarget.create("https://example.com/health", 29));
        assertThrows(IllegalArgumentException.class,
                () -> MonitorTarget.create("https://example.com/health", 3601));
    }
}
