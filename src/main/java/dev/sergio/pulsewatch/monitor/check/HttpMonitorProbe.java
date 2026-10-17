package dev.sergio.pulsewatch.monitor.check;

import dev.sergio.pulsewatch.monitor.domain.MonitorStatus;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

@Component
public class HttpMonitorProbe implements MonitorProbe {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final PublicTargetAddressPolicy targetAddressPolicy;
    private final OkHttpClient httpClient;

    public HttpMonitorProbe(PublicTargetAddressPolicy targetAddressPolicy) {
        this.targetAddressPolicy = targetAddressPolicy;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(REQUEST_TIMEOUT)
                .callTimeout(REQUEST_TIMEOUT)
                .followRedirects(false)
                .followSslRedirects(false)
                .proxy(Proxy.NO_PROXY)
                .build();
    }

    @Override
    public ProbeResult probe(URI endpoint) {
        List<InetAddress> validatedAddresses = targetAddressPolicy.resolvePublicAddresses(endpoint);
        long startedAt = System.nanoTime();
        String validatedHost = normalizeHost(endpoint.getHost());

        Dns pinnedDns = hostname -> {
            if (!normalizeHost(hostname).equals(validatedHost)) {
                throw new UnknownHostException("El nombre solicitado no coincide con el host validado");
            }
            return validatedAddresses;
        };

        try {
            Request request = new Request.Builder()
                    .url(endpoint.toString())
                    .header("User-Agent", "PulseWatch/0.1")
                    .get()
                    .build();
            OkHttpClient requestClient = httpClient.newBuilder()
                    .dns(pinnedDns)
                    .build();

            try (Response response = requestClient.newCall(request).execute()) {
                long durationMs = elapsedMillis(startedAt);
                int httpStatus = response.code();
                MonitorStatus status = httpStatus >= 200 && httpStatus < 300
                        ? MonitorStatus.UP
                        : MonitorStatus.DOWN;
                return new ProbeResult(status, httpStatus, durationMs, Instant.now());
            }
        } catch (IOException exception) {
            return failedProbe(startedAt);
        }
    }

    private String normalizeHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    private ProbeResult failedProbe(long startedAt) {
        return new ProbeResult(MonitorStatus.DOWN, null, elapsedMillis(startedAt), Instant.now());
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
