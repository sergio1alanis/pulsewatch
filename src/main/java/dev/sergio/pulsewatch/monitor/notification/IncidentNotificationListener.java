package dev.sergio.pulsewatch.monitor.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IncidentNotificationListener {

    private final IncidentNotifier incidentNotifier;

    public IncidentNotificationListener(IncidentNotifier incidentNotifier) {
        this.incidentNotifier = incidentNotifier;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIncidentNotification(IncidentNotification notification) {
        incidentNotifier.send(notification);
    }
}
