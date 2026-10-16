package dev.sergio.pulsewatch.monitor.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorIncidentRepository extends JpaRepository<MonitorIncidentEntity, UUID> {

    Optional<MonitorIncidentEntity> findFirstByMonitor_IdAndResolvedAtIsNullOrderByStartedAtDescIdDesc(
            UUID monitorId);

    List<MonitorIncidentEntity> findTop100ByMonitor_IdOrderByStartedAtDescIdDesc(UUID monitorId);
}
