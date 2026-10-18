package dev.sergio.pulsewatch.monitor.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MonitorCheckRepository extends JpaRepository<MonitorCheckEntity, Long> {

    List<MonitorCheckEntity> findTop100ByMonitor_IdOrderByCheckedAtDescIdDesc(UUID monitorId);

    @Modifying
    @Query("delete from MonitorCheckEntity c where c.monitor.id = :monitorId")
    int deleteAllByMonitorId(@Param("monitorId") UUID monitorId);
}
