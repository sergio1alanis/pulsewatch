package dev.sergio.pulsewatch.monitor.notification;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class WebhookIncidentNotifier implements IncidentNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookIncidentNotifier.class);

    private final URI webhookUri;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WebhookIncidentNotifier(
            @Value("${pulsewatch.notifications.webhook-url:}") String webhookUrl,
            ObjectMapper objectMapper) {
        this.webhookUri = parseWebhookUri(webhookUrl);
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public void send(IncidentNotification notification) {
        if (webhookUri == null) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(notification);
            HttpRequest request = HttpRequest.newBuilder(webhookUri)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<Void> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn(
                        "Incident webhook returned HTTP {} for event {} and incident {}",
                        response.statusCode(), notification.event(), notification.incidentId());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn(
                    "Incident webhook delivery was interrupted for incident {}",
                    notification.incidentId());
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn(
                    "Incident webhook delivery failed for incident {} ({})",
                    notification.incidentId(), exception.getClass().getSimpleName());
        }
    }

    private static URI parseWebhookUri(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return null;
        }

        URI uri = URI.create(webhookUrl);
        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.toLowerCase(Locale.ROOT).equals("http")
                || scheme.toLowerCase(Locale.ROOT).equals("https"))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "PULSEWATCH_WEBHOOK_URL debe ser una URL HTTP(S) válida sin credenciales ni fragmento");
        }
        return uri;
    }
}
