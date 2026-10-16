package dev.sergio.pulsewatch.monitor.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class WebhookIncidentNotifierTest {

    @Test
    void postsIncidentEventAsJson() throws Exception {
        AtomicReference<String> requestMethod = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            WebhookIncidentNotifier notifier = new WebhookIncidentNotifier(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                    objectMapper);
            UUID incidentId = UUID.randomUUID();
            UUID monitorId = UUID.randomUUID();
            String occurredAt = "2026-01-01T00:00:00Z";

            notifier.send(new IncidentNotification(
                    "INCIDENT_OPENED",
                    incidentId,
                    monitorId,
                    "Payments API",
                    "https://payments.example/health",
                    occurredAt,
                    occurredAt,
                    null));

            JsonNode payload = objectMapper.readTree(requestBody.get());
            assertEquals("POST", requestMethod.get());
            assertEquals("INCIDENT_OPENED", payload.path("event").asText());
            assertEquals(incidentId.toString(), payload.path("incidentId").asText());
            assertEquals(monitorId.toString(), payload.path("monitorId").asText());
            assertEquals("Payments API", payload.path("monitorName").asText());
            assertEquals("https://payments.example/health", payload.path("endpoint").asText());
            assertEquals(occurredAt, payload.path("startedAt").asText());
            assertTrue(payload.get("resolvedAt").isNull());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void webhookFailureDoesNotThrowToIncidentFlow() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        try {
            WebhookIncidentNotifier notifier = new WebhookIncidentNotifier(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                    new ObjectMapper());
            String occurredAt = "2026-01-01T00:00:00Z";
            IncidentNotification notification = new IncidentNotification(
                    "INCIDENT_RESOLVED",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Payments API",
                    "https://payments.example/health",
                    occurredAt,
                    occurredAt,
                    occurredAt);

            assertDoesNotThrow(() -> notifier.send(notification));
        } finally {
            server.stop(0);
        }
    }
}
