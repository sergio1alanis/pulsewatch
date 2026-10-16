package dev.sergio.pulsewatch.monitor.persistence;

import dev.sergio.pulsewatch.monitor.domain.MonitorStatus;
import dev.sergio.pulsewatch.monitor.domain.MonitorTarget;
import dev.sergio.pulsewatch.monitor.check.ProbeResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monitors")
public class MonitorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 2048)
    private String endpoint;

    @Column(name = "interval_seconds", nullable = false)
    private int intervalSeconds;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MonitorStatus status = MonitorStatus.UNKNOWN;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_http_status")
    private Integer lastHttpStatus;

    @Column(name = "last_response_time_ms")
    private Long lastResponseTimeMs;

    protected MonitorEntity() {
        // JPA requiere un constructor sin argumentos.
    }

    public MonitorEntity(String name, MonitorTarget target) {
        this.name = name;
        this.endpoint = target.endpoint().toASCIIString();
        this.intervalSeconds = Math.toIntExact(target.checkInterval().toSeconds());
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void updateDetails(String name, MonitorTarget target) {
        this.name = name;
        this.endpoint = target.endpoint().toASCIIString();
        this.intervalSeconds = Math.toIntExact(target.checkInterval().toSeconds());
    }

    public MonitorStatus getStatus() {
        return status;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public Integer getLastHttpStatus() {
        return lastHttpStatus;
    }

    public Long getLastResponseTimeMs() {
        return lastResponseTimeMs;
    }

    public void recordProbe(ProbeResult result) {
        this.status = result.status();
        this.lastHttpStatus = result.httpStatus();
        this.lastResponseTimeMs = result.responseTimeMs();
        this.lastCheckedAt = result.checkedAt();
    }

    public boolean isDueAt(Instant now) {
        return enabled
                && (lastCheckedAt == null
                || !lastCheckedAt.plusSeconds(intervalSeconds).isAfter(now));
    }
}
