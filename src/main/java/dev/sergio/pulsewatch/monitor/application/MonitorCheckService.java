package dev.sergio.pulsewatch.monitor.application;

import dev.sergio.pulsewatch.monitor.check.MonitorProbe;
import dev.sergio.pulsewatch.monitor.check.ProbeResult;
import dev.sergio.pulsewatch.monitor.domain.MonitorStatus;
import dev.sergio.pulsewatch.monitor.persistence.MonitorCheckEntity;
import dev.sergio.pulsewatch.monitor.persistence.MonitorCheckRepository;
import dev.sergio.pulsewatch.monitor.persistence.MonitorEntity;
import dev.sergio.pulsewatch.monitor.persistence.MonitorRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitorCheckService {

    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository monitorCheckRepository;
    private final MonitorProbe monitorProbe;
    private final MonitorIncidentService monitorIncidentService;

    public MonitorCheckService(
            MonitorRepository monitorRepository,
            MonitorCheckRepository monitorCheckRepository,
            MonitorProbe monitorProbe,
            MonitorIncidentService monitorIncidentService) {
        this.monitorRepository = monitorRepository;
        this.monitorCheckRepository = monitorCheckRepository;
        this.monitorProbe = monitorProbe;
        this.monitorIncidentService = monitorIncidentService;
    }

    @Transactional
    public MonitorView checkNow(UUID monitorId) {
        MonitorEntity monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new MonitorNotFoundException(monitorId));
        ProbeResult result = monitorProbe.probe(URI.create(monitor.getEndpoint()));
        monitor.recordProbe(result);
        monitorCheckRepository.save(new MonitorCheckEntity(monitor, result));
        monitorIncidentService.recordProbeOutcome(monitor, result);
        return MonitorView.from(monitor);
    }

    @Transactional
    public void recordPolicyRejection(UUID monitorId, Instant attemptedAt) {
        MonitorEntity monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new MonitorNotFoundException(monitorId));
        ProbeResult blockedResult = new ProbeResult(
                MonitorStatus.BLOCKED, null, 0, attemptedAt);
        monitor.recordProbe(blockedResult);
        monitorCheckRepository.save(new MonitorCheckEntity(monitor, blockedResult));
        monitorIncidentService.recordProbeOutcome(monitor, blockedResult);
    }

    @Transactional(readOnly = true)
    public List<MonitorCheckView> history(UUID monitorId) {
        if (!monitorRepository.existsById(monitorId)) {
            throw new MonitorNotFoundException(monitorId);
        }
        return monitorCheckRepository
                .findTop100ByMonitor_IdOrderByCheckedAtDescIdDesc(monitorId)
                .stream()
                .map(check -> MonitorCheckView.from(check, monitorId))
                .toList();
    }
}
