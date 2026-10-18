package dev.sergio.pulsewatch.monitor.check;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sergio.pulsewatch.monitor.application.MonitorCheckService;
import dev.sergio.pulsewatch.monitor.application.MonitorNotFoundException;
import dev.sergio.pulsewatch.monitor.persistence.MonitorEntity;
import dev.sergio.pulsewatch.monitor.persistence.MonitorRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MonitorSchedulerTest {

    @Test
    void runsOnlyEnabledMonitorsThatAreDue() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UUID dueId = UUID.randomUUID();
        UUID notDueId = UUID.randomUUID();
        UUID disabledId = UUID.randomUUID();
        MonitorEntity due = monitor(dueId, true, true, now);
        MonitorEntity notDue = monitor(notDueId, true, false, now);
        MonitorEntity disabled = monitor(disabledId, false, true, now);
        MonitorRepository repository = mock(MonitorRepository.class);
        MonitorCheckService checkService = mock(MonitorCheckService.class);
        when(repository.findByEnabledTrueOrderByNameAsc())
                .thenReturn(List.of(due, notDue, disabled));

        new MonitorScheduler(repository, checkService).runDueChecks(now);

        verify(checkService).checkNow(dueId);
        verify(checkService, never()).checkNow(notDueId);
        verify(checkService, never()).checkNow(disabledId);
    }

    @Test
    void recordsRejectedTargetAndContinuesCheckingOtherMonitors() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UUID blockedId = UUID.randomUUID();
        UUID validId = UUID.randomUUID();
        MonitorRepository repository = mock(MonitorRepository.class);
        MonitorCheckService checkService = mock(MonitorCheckService.class);
        MonitorEntity blocked = monitor(blockedId, true, true, now);
        MonitorEntity valid = monitor(validId, true, true, now);
        when(repository.findByEnabledTrueOrderByNameAsc())
                .thenReturn(List.of(blocked, valid));
        when(checkService.checkNow(blockedId))
                .thenThrow(new IllegalArgumentException("non-public destination"));

        new MonitorScheduler(repository, checkService).runDueChecks(now);

        verify(checkService).recordPolicyRejection(blockedId, now);
        verify(checkService).checkNow(validId);
    }

    @Test
    void continuesCheckingWhenMonitorIsDeletedBeforeItsScheduledCheck() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UUID deletedId = UUID.randomUUID();
        UUID remainingId = UUID.randomUUID();
        MonitorRepository repository = mock(MonitorRepository.class);
        MonitorCheckService checkService = mock(MonitorCheckService.class);
        MonitorEntity deleted = monitor(deletedId, true, true, now);
        MonitorEntity remaining = monitor(remainingId, true, true, now);
        when(repository.findByEnabledTrueOrderByNameAsc()).thenReturn(List.of(deleted, remaining));
        when(checkService.checkNow(deletedId)).thenThrow(new MonitorNotFoundException(deletedId));

        new MonitorScheduler(repository, checkService).runDueChecks(now);

        verify(checkService).checkNow(remainingId);
    }

    @Test
    void continuesCheckingWhenMonitorIsDeletedBeforePolicyRejectionIsRecorded() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UUID deletedId = UUID.randomUUID();
        UUID remainingId = UUID.randomUUID();
        MonitorRepository repository = mock(MonitorRepository.class);
        MonitorCheckService checkService = mock(MonitorCheckService.class);
        MonitorEntity deleted = monitor(deletedId, true, true, now);
        MonitorEntity remaining = monitor(remainingId, true, true, now);
        when(repository.findByEnabledTrueOrderByNameAsc()).thenReturn(List.of(deleted, remaining));
        when(checkService.checkNow(deletedId))
                .thenThrow(new IllegalArgumentException("non-public destination"));
        doThrow(new MonitorNotFoundException(deletedId))
                .when(checkService)
                .recordPolicyRejection(deletedId, now);

        new MonitorScheduler(repository, checkService).runDueChecks(now);

        verify(checkService).recordPolicyRejection(deletedId, now);
        verify(checkService).checkNow(remainingId);
    }

    private MonitorEntity monitor(UUID id, boolean enabled, boolean due, Instant now) {
        MonitorEntity monitor = mock(MonitorEntity.class);
        when(monitor.getId()).thenReturn(id);
        when(monitor.isEnabled()).thenReturn(enabled);
        when(monitor.isDueAt(now)).thenReturn(due);
        return monitor;
    }
}
