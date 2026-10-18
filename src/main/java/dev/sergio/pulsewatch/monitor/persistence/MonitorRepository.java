package dev.sergio.pulsewatch.monitor.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MonitorRepository extends JpaRepository<MonitorEntity, UUID> {

    List<MonitorEntity> findAllByOrderByNameAsc();

    List<MonitorEntity> findByEnabledTrueOrderByNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select monitor from MonitorEntity monitor where monitor.id = :monitorId")
    Optional<MonitorEntity> findByIdForUpdate(@Param("monitorId") UUID monitorId);
}
