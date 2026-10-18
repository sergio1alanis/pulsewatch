package dev.sergio.pulsewatch.monitor.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MonitorIncidentRepository extends JpaRepository<MonitorIncidentEntity, UUID> {

    Optional<MonitorIncidentEntity> findFirstByMonitor_IdAndResolvedAtIsNullOrderByStartedAtDescIdDesc(
            UUID monitorId);

    List<MonitorIncidentEntity> findTop100ByMonitor_IdOrderByStartedAtDescIdDesc(UUID monitorId);

    @Modifying
    @Query("delete from MonitorIncidentEntity i where i.monitor.id = :monitorId")
    int deleteAllByMonitorId(@Param("monitorId") UUID monitorId);
}
