package dev.sergio.pulsewatch.monitor.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monitor_incidents", indexes = {
        @Index(name = "idx_monitor_incidents_monitor_started_at", columnList = "monitor_id, started_at")
})
public class MonitorIncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private MonitorEntity monitor;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "last_observed_at", nullable = false)
    private Instant lastObservedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected MonitorIncidentEntity() {
        // JPA requiere un constructor sin argumentos.
    }

    public MonitorIncidentEntity(MonitorEntity monitor, Instant startedAt) {
        this.monitor = monitor;
        this.startedAt = startedAt;
        this.lastObservedAt = startedAt;
    }

    public UUID getId() {
        return id;
    }

    public MonitorEntity getMonitor() {
        return monitor;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getLastObservedAt() {
        return lastObservedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void observeDownAt(Instant observedAt) {
        if (observedAt.isAfter(lastObservedAt)) {
            lastObservedAt = observedAt;
        }
    }

    public void resolveAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt.isBefore(startedAt) ? startedAt : resolvedAt;
    }
}
