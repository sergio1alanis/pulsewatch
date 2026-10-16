package dev.sergio.pulsewatch.monitor.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMonitorRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 2048) String endpoint,
        int intervalSeconds) {
}
