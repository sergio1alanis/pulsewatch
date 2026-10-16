package dev.sergio.pulsewatch.monitor.persistence;

import dev.sergio.pulsewatch.monitor.check.ProbeResult;
import dev.sergio.pulsewatch.monitor.domain.MonitorStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "monitor_checks", indexes = {
        @Index(name = "idx_monitor_checks_monitor_checked_at", columnList = "monitor_id, checked_at")
})
public class MonitorCheckEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private MonitorEntity monitor;

    @Column(nullable = false, length = 2048)
    private String endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MonitorStatus status;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_time_ms", nullable = false)
    private long responseTimeMs;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    protected MonitorCheckEntity() {
        // JPA requiere un constructor sin argumentos.
    }

    public MonitorCheckEntity(MonitorEntity monitor, ProbeResult result) {
        this.monitor = monitor;
        this.endpoint = monitor.getEndpoint();
        this.status = result.status();
        this.httpStatus = result.httpStatus();
        this.responseTimeMs = result.responseTimeMs();
        this.checkedAt = result.checkedAt();
    }

    public Long getId() {
        return id;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public MonitorStatus getStatus() {
        return status;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }
}
