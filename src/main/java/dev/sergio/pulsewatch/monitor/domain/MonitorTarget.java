package dev.sergio.pulsewatch.monitor.domain;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

/** Reglas de dominio para un destino y la frecuencia de su monitoreo. */
public record MonitorTarget(URI endpoint, Duration checkInterval) {

    private static final long MIN_INTERVAL_SECONDS = 30;
    private static final long MAX_INTERVAL_SECONDS = 3_600;

    public MonitorTarget {
        Objects.requireNonNull(endpoint, "El endpoint es obligatorio");
        Objects.requireNonNull(checkInterval, "El intervalo es obligatorio");

        endpoint = endpoint.normalize();
        String scheme = endpoint.getScheme();
        if (scheme == null
                || !(scheme.toLowerCase(Locale.ROOT).equals("http")
                || scheme.toLowerCase(Locale.ROOT).equals("https"))) {
            throw new IllegalArgumentException("El endpoint debe usar HTTP o HTTPS");
        }
        if (endpoint.getHost() == null || endpoint.getHost().isBlank()) {
            throw new IllegalArgumentException("El endpoint debe incluir un host válido");
        }
        if (endpoint.getUserInfo() != null) {
            throw new IllegalArgumentException("El endpoint no debe incluir credenciales");
        }
        if (checkInterval.getNano() != 0
                || checkInterval.getSeconds() < MIN_INTERVAL_SECONDS
                || checkInterval.getSeconds() > MAX_INTERVAL_SECONDS) {
            throw new IllegalArgumentException(
                    "El intervalo debe estar entre 30 y 3600 segundos");
        }
    }

    public static MonitorTarget create(String rawEndpoint, int intervalSeconds) {
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            throw new IllegalArgumentException("El endpoint es obligatorio");
        }

        URI uri = URI.create(rawEndpoint.trim());
        return new MonitorTarget(uri, Duration.ofSeconds(intervalSeconds));
    }
}
