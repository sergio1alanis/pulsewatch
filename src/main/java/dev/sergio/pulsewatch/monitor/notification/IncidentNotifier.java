package dev.sergio.pulsewatch.monitor.notification;

public interface IncidentNotifier {

    void send(IncidentNotification notification);
}
