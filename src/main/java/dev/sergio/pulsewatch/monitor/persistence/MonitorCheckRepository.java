package dev.sergio.pulsewatch.monitor.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorCheckRepository extends JpaRepository<MonitorCheckEntity, Long> {

    List<MonitorCheckEntity> findTop100ByMonitor_IdOrderByCheckedAtDescIdDesc(UUID monitorId);
}
