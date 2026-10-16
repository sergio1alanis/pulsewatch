package dev.sergio.pulsewatch.monitor.application;

import dev.sergio.pulsewatch.monitor.check.ProbeResult;
import dev.sergio.pulsewatch.monitor.domain.MonitorStatus;
import dev.sergio.pulsewatch.monitor.persistence.MonitorEntity;
import dev.sergio.pulsewatch.monitor.persistence.MonitorIncidentEntity;
import dev.sergio.pulsewatch.monitor.persistence.MonitorIncidentRepository;
import dev.sergio.pulsewatch.monitor.persistence.MonitorRepository;
import dev.sergio.pulsewatch.monitor.notification.IncidentNotification;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitorIncidentService {

    private final MonitorIncidentRepository incidentRepository;
    private final MonitorRepository monitorRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MonitorIncidentService(
            MonitorIncidentRepository incidentRepository,
            MonitorRepository monitorRepository,
            ApplicationEventPublisher eventPublisher) {
        this.incidentRepository = incidentRepository;
        this.monitorRepository = monitorRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void recordProbeOutcome(MonitorEntity monitor, ProbeResult result) {
        if (result.status() == MonitorStatus.DOWN) {
            incidentRepository
                    .findFirstByMonitor_IdAndResolvedAtIsNullOrderByStartedAtDescIdDesc(monitor.getId())
                    .ifPresentOrElse(
                            incident -> incident.observeDownAt(result.checkedAt()),
                            () -> {
                                MonitorIncidentEntity incident = incidentRepository.save(
                                        new MonitorIncidentEntity(monitor, result.checkedAt()));
                                eventPublisher.publishEvent(
                                        IncidentNotification.from("INCIDENT_OPENED", incident));
                            });
        } else if (result.status() == MonitorStatus.UP) {
            incidentRepository
                    .findFirstByMonitor_IdAndResolvedAtIsNullOrderByStartedAtDescIdDesc(monitor.getId())
                    .ifPresent(incident -> {
                        incident.resolveAt(result.checkedAt());
                        eventPublisher.publishEvent(
                                IncidentNotification.from("INCIDENT_RESOLVED", incident));
                    });
        }
        // UNKNOWN y BLOCKED no representan una observación de disponibilidad.
    }

    @Transactional(readOnly = true)
    public List<MonitorIncidentView> history(UUID monitorId) {
        if (!monitorRepository.existsById(monitorId)) {
            throw new MonitorNotFoundException(monitorId);
        }
        return incidentRepository.findTop100ByMonitor_IdOrderByStartedAtDescIdDesc(monitorId)
                .stream()
                .map(MonitorIncidentView::from)
                .toList();
    }
}
