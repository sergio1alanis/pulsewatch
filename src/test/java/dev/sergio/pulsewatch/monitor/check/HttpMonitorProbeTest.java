package dev.sergio.pulsewatch.monitor.check;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import dev.sergio.pulsewatch.monitor.domain.MonitorStatus;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class HttpMonitorProbeTest {

    @Test
    void connectsOnlyToTheAddressValidatedForTheOriginalHost() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        try {
            InetAddress pinnedAddress = InetAddress.getByName("127.0.0.1");
            PublicTargetAddressPolicy policy = new PublicTargetAddressPolicy(false) {
                @Override
                public List<InetAddress> resolvePublicAddresses(URI target) {
                    return List.of(pinnedAddress);
                }
            };
            HttpMonitorProbe probe = new HttpMonitorProbe(policy);
            URI endpoint = URI.create(
                    "http://rebind-test.invalid:" + server.getAddress().getPort() + "/health");

            ProbeResult result = probe.probe(endpoint);

            assertEquals(MonitorStatus.UP, result.status());
            assertEquals(204, result.httpStatus());
            assertEquals(1, requestCount.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void doesNotFollowRedirectToAnUnvalidatedDestination() throws Exception {
        AtomicInteger internalRequestCount = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + port + "/internal");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/internal", exchange -> {
            internalRequestCount.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            InetAddress pinnedAddress = InetAddress.getByName("127.0.0.1");
            PublicTargetAddressPolicy policy = new PublicTargetAddressPolicy(false) {
                @Override
                public List<InetAddress> resolvePublicAddresses(URI target) {
                    return List.of(pinnedAddress);
                }
            };
            HttpMonitorProbe probe = new HttpMonitorProbe(policy);
            URI endpoint = URI.create("http://rebind-test.invalid:" + port + "/redirect");

            ProbeResult result = probe.probe(endpoint);

            assertEquals(MonitorStatus.DOWN, result.status());
            assertEquals(302, result.httpStatus());
            assertEquals(0, internalRequestCount.get());
        } finally {
            server.stop(0);
        }
    }
}
