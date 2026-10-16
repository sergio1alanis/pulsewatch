package dev.sergio.pulsewatch.monitor.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorRepository extends JpaRepository<MonitorEntity, UUID> {

    List<MonitorEntity> findAllByOrderByNameAsc();

    List<MonitorEntity> findByEnabledTrueOrderByNameAsc();
}
