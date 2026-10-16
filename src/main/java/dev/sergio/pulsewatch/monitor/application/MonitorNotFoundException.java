package dev.sergio.pulsewatch.monitor.application;

import java.util.UUID;

public class MonitorNotFoundException extends RuntimeException {

    public MonitorNotFoundException(UUID id) {
        super("Monitor no encontrado: " + id);
    }
}
