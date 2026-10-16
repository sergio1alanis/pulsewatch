package dev.sergio.pulsewatch.monitor.check;

import dev.sergio.pulsewatch.monitor.domain.MonitorStatus;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class HttpMonitorProbe implements MonitorProbe {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final PublicTargetAddressPolicy targetAddressPolicy;
    private final HttpClient httpClient;

    public HttpMonitorProbe(PublicTargetAddressPolicy targetAddressPolicy) {
        this.targetAddressPolicy = targetAddressPolicy;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public ProbeResult probe(URI endpoint) {
        targetAddressPolicy.validate(endpoint);
        long startedAt = System.nanoTime();

        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "PulseWatch/0.1")
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.discarding());
            long durationMs = elapsedMillis(startedAt);
            MonitorStatus status = response.statusCode() >= 200 && response.statusCode() < 300
                    ? MonitorStatus.UP
                    : MonitorStatus.DOWN;
            return new ProbeResult(status, response.statusCode(), durationMs, Instant.now());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failedProbe(startedAt);
        } catch (IOException exception) {
            return failedProbe(startedAt);
        }
    }

    private ProbeResult failedProbe(long startedAt) {
        return new ProbeResult(MonitorStatus.DOWN, null, elapsedMillis(startedAt), Instant.now());
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
