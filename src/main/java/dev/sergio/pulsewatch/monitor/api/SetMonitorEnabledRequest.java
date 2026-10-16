package dev.sergio.pulsewatch.monitor.api;

import jakarta.validation.constraints.NotNull;

public record SetMonitorEnabledRequest(@NotNull Boolean enabled) {
}
