package dev.sergio.pulsewatch.monitor.check;

import dev.sergio.pulsewatch.monitor.application.MonitorCheckService;
import dev.sergio.pulsewatch.monitor.persistence.MonitorEntity;
import dev.sergio.pulsewatch.monitor.persistence.MonitorRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "pulsewatch.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MonitorScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorScheduler.class);

    private final MonitorRepository monitorRepository;
    private final MonitorCheckService monitorCheckService;

    public MonitorScheduler(
            MonitorRepository monitorRepository,
            MonitorCheckService monitorCheckService) {
        this.monitorRepository = monitorRepository;
        this.monitorCheckService = monitorCheckService;
    }

    @Scheduled(
            fixedDelayString = "${pulsewatch.scheduler.fixed-delay-ms:5000}",
            initialDelayString = "${pulsewatch.scheduler.initial-delay-ms:10000}")
    public void scheduledTick() {
        runDueChecks(Instant.now());
    }

    public void runDueChecks(Instant now) {
        for (MonitorEntity monitor : monitorRepository.findByEnabledTrueOrderByNameAsc()) {
            if (!monitor.isEnabled() || !monitor.isDueAt(now)) {
                continue;
            }

            try {
                monitorCheckService.checkNow(monitor.getId());
            } catch (IllegalArgumentException exception) {
                monitorCheckService.recordPolicyRejection(monitor.getId(), now);
                LOGGER.warn(
                        "Monitor {} was blocked by the target policy; it will retry after its interval",
                        monitor.getId());
            }
        }
    }
}
