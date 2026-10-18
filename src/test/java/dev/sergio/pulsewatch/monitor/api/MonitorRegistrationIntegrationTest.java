package dev.sergio.pulsewatch.monitor.api;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class MonitorRegistrationIntegrationTest {

    private static final ConcurrentLinkedQueue<String> WEBHOOK_PAYLOADS = new ConcurrentLinkedQueue<>();
    private static final HttpServer WEBHOOK_SERVER = startWebhookServer();

    @DynamicPropertySource
    static void configureWebhook(DynamicPropertyRegistry registry) {
        registry.add(
                "pulsewatch.notifications.webhook-url",
                () -> "http://127.0.0.1:" + WEBHOOK_SERVER.getAddress().getPort() + "/webhook");
    }

    private static HttpServer startWebhookServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/webhook", exchange -> {
                WEBHOOK_PAYLOADS.add(new String(
                        exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @AfterAll
    static void stopWebhookServer() {
        WEBHOOK_SERVER.stop(0);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        WEBHOOK_PAYLOADS.clear();
        jdbcTemplate.update("DELETE FROM monitor_incidents");
        jdbcTemplate.update("DELETE FROM monitor_checks");
        jdbcTemplate.update("DELETE FROM monitors");
    }

    @Test
    void exposesOpenApiDocumentForMonitorAndIncidentEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("PulseWatch API"))
                .andExpect(jsonPath("$.paths['/api/v1/monitors']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/monitors/{id}/incidents']").exists());
    }

    @Test
    void servesDashboardWithoutApiKey() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_HTML))
                .andExpect(content().string(containsString("PulseWatch Dashboard")));
    }

    @Test
    void registersMonitorAndPersistsIt() throws Exception {
        mockMvc.perform(post("/api/v1/monitors")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Payments API",
                                  "endpoint": "https://example.com/health",
                                  "intervalSeconds": 60
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Payments API"))
                .andExpect(jsonPath("$.endpoint").value("https://example.com/health"))
                .andExpect(jsonPath("$.intervalSeconds").value(60))
                .andExpect(jsonPath("$.status").value("UNKNOWN"));

        Integer persistedMonitors = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM monitors", Integer.class);
        assertEquals(1, persistedMonitors);
    }

    @Test
    void rejectsNonHttpEndpointsWithoutPersistingThem() throws Exception {
        mockMvc.perform(post("/api/v1/monitors")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unsafe API",
                                  "endpoint": "ftp://example.com/health",
                                  "intervalSeconds": 60
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El monitor contiene datos inválidos"));

        Integer persistedMonitors = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM monitors", Integer.class);
        assertEquals(0, persistedMonitors);
    }

    @Test
    void listsPersistedMonitorsInNameOrder() throws Exception {
        registerMonitor("Zulu API", "https://zulu.example/health");
        registerMonitor("Alpha API", "https://alpha.example/health");

        mockMvc.perform(get("/api/v1/monitors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha API"))
                .andExpect(jsonPath("$[1].name").value("Zulu API"))
                .andExpect(jsonPath("$[0].status").value("UNKNOWN"));
    }

    @Test
    void getsPersistedMonitorById() throws Exception {
        String id = registerMonitor("Payments API", "https://payments.example/health");

        mockMvc.perform(get("/api/v1/monitors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Payments API"))
                .andExpect(jsonPath("$.endpoint").value("https://payments.example/health"))
                .andExpect(jsonPath("$.intervalSeconds").value(60))
                .andExpect(jsonPath("$.status").value("UNKNOWN"));
    }

    @Test
    void returnsProblemDetailWhenMonitorDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/monitors/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe un monitor con ese ID"));
    }

    @Test
    void pausesAndResumesMonitorAndPersistsEnabledState() throws Exception {
        String id = registerMonitor("Background API", "https://background.example/health");

        mockMvc.perform(get("/api/v1/monitors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(patch("/api/v1/monitors/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "enabled": false }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.status").value("UNKNOWN"));

        mockMvc.perform(get("/api/v1/monitors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(patch("/api/v1/monitors/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "enabled": true }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void updatesMonitorDetailsAndPersistsThem() throws Exception {
        String id = registerMonitor("Old API name", "https://old.example/health");
        mockMvc.perform(patch("/api/v1/monitors/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "enabled": false }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/monitors/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated API",
                                  "endpoint": "https://updated.example/health",
                                  "intervalSeconds": 300
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated API"))
                .andExpect(jsonPath("$.endpoint").value("https://updated.example/health"))
                .andExpect(jsonPath("$.intervalSeconds").value(300))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.status").value("UNKNOWN"));

        mockMvc.perform(get("/api/v1/monitors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated API"))
                .andExpect(jsonPath("$.intervalSeconds").value(300))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void checksHttpEndpointAndPersistsLatestResult() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/health";
            String id = registerMonitor("Local test API", endpoint);

            mockMvc.perform(post("/api/v1/monitors/{id}/checks", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.lastHttpStatus").value(204))
                    .andExpect(jsonPath("$.lastResponseTimeMs").isNumber())
                    .andExpect(jsonPath("$.lastCheckedAt").isNotEmpty());

            mockMvc.perform(get("/api/v1/monitors/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.lastHttpStatus").value(204));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void storesAndListsRecentCheckHistoryNewestFirst() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> {
            int statusCode = requestCount.incrementAndGet() == 1 ? 204 : 503;
            exchange.sendResponseHeaders(statusCode, -1);
            exchange.close();
        });
        server.start();

        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/health";
            String id = registerMonitor("History API", endpoint);

            mockMvc.perform(post("/api/v1/monitors/{id}/checks", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
            mockMvc.perform(post("/api/v1/monitors/{id}/checks", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DOWN"));

            mockMvc.perform(get("/api/v1/monitors/{id}/checks", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].status").value("DOWN"))
                    .andExpect(jsonPath("$[0].httpStatus").value(503))
                    .andExpect(jsonPath("$[1].status").value("UP"))
                    .andExpect(jsonPath("$[1].httpStatus").value(204));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void opensOneIncidentDuringAnOutageAndResolvesItOnRecovery() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> {
            int attempt = requestCount.incrementAndGet();
            int statusCode = attempt == 3 ? 204 : 503;
            exchange.sendResponseHeaders(statusCode, -1);
            exchange.close();
        });
        server.start();

        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/health";
            String id = registerMonitor("Incident API", endpoint);

            mockMvc.perform(post("/api/v1/monitors/{id}/checks", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DOWN"));
            mockMvc.perform(post("/api/v1/monitors/{id}/checks", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DOWN"));

            mockMvc.perform(get("/api/v1/monitors/{id}/incidents", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].status").value("OPEN"))
                    .andExpect(jsonPath("$[0].startedAt").isNotEmpty())
                    .andExpect(jsonPath("$[0].lastObservedAt").isNotEmpty());

            mockMvc.perform(post("/api/v1/monitors/{id}/checks", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));

            mockMvc.perform(get("/api/v1/monitors/{id}/incidents", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].status").value("RESOLVED"))
                    .andExpect(jsonPath("$[0].resolvedAt").isNotEmpty());

            mockMvc.perform(post("/api/v1/monitors/{id}/checks", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DOWN"));
            mockMvc.perform(get("/api/v1/monitors/{id}/incidents", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].status").value("OPEN"))
                    .andExpect(jsonPath("$[1].status").value("RESOLVED"));

            String[] webhookEvents = WEBHOOK_PAYLOADS.toArray(String[]::new);
            assertEquals(3, webhookEvents.length);
            assertTrue(webhookEvents[0].contains("\"event\":\"INCIDENT_OPENED\""));
            assertTrue(webhookEvents[1].contains("\"event\":\"INCIDENT_RESOLVED\""));
            assertTrue(webhookEvents[2].contains("\"event\":\"INCIDENT_OPENED\""));
        } finally {
            server.stop(0);
        }
    }

    private String registerMonitor(String name, String endpoint) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/monitors")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "endpoint": "%s",
                                  "intervalSeconds": 60
                                }
                                """.formatted(name, endpoint)))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
