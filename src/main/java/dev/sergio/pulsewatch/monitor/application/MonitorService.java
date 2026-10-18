package dev.sergio.pulsewatch.monitor.application;

import dev.sergio.pulsewatch.monitor.domain.MonitorTarget;
import dev.sergio.pulsewatch.monitor.persistence.MonitorCheckRepository;
import dev.sergio.pulsewatch.monitor.persistence.MonitorEntity;
import dev.sergio.pulsewatch.monitor.persistence.MonitorIncidentRepository;
import dev.sergio.pulsewatch.monitor.persistence.MonitorRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository monitorCheckRepository;
    private final MonitorIncidentRepository monitorIncidentRepository;

    public MonitorService(
            MonitorRepository monitorRepository,
            MonitorCheckRepository monitorCheckRepository,
            MonitorIncidentRepository monitorIncidentRepository) {
        this.monitorRepository = monitorRepository;
        this.monitorCheckRepository = monitorCheckRepository;
        this.monitorIncidentRepository = monitorIncidentRepository;
    }

    @Transactional
    public MonitorView register(String name, String endpoint, int intervalSeconds) {
        MonitorTarget target = MonitorTarget.create(endpoint, intervalSeconds);
        MonitorEntity monitor = monitorRepository.save(new MonitorEntity(name.trim(), target));

        return toView(monitor);
    }

    @Transactional(readOnly = true)
    public List<MonitorView> list() {
        return monitorRepository.findAllByOrderByNameAsc().stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public MonitorView findById(UUID id) {
        return monitorRepository.findById(id)
                .map(this::toView)
                .orElseThrow(() -> new MonitorNotFoundException(id));
    }

    @Transactional
    public MonitorView setEnabled(UUID id, boolean enabled) {
        MonitorEntity monitor = monitorRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new MonitorNotFoundException(id));
        monitor.setEnabled(enabled);
        return toView(monitor);
    }

    @Transactional
    public MonitorView updateDetails(
            UUID id, String name, String endpoint, int intervalSeconds) {
        MonitorTarget target = MonitorTarget.create(endpoint, intervalSeconds);
        MonitorEntity monitor = monitorRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new MonitorNotFoundException(id));
        monitor.updateDetails(name.trim(), target);
        return toView(monitor);
    }

    @Transactional
    public void delete(UUID id) {
        MonitorEntity monitor = monitorRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new MonitorNotFoundException(id));
        monitorIncidentRepository.deleteAllByMonitorId(id);
        monitorCheckRepository.deleteAllByMonitorId(id);
        monitorRepository.delete(monitor);
    }

    private MonitorView toView(MonitorEntity monitor) {
        return MonitorView.from(monitor);
    }
}
