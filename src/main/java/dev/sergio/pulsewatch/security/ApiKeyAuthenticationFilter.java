package dev.sergio.pulsewatch.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public final class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    private static final long RATE_WINDOW_NANOS = Duration.ofMinutes(1).toNanos();

    private final byte[] configuredApiKey;
    private final boolean apiKeyConfigured;
    private final int maxRequestsPerMinute;
    private long windowStartedAtNanos = System.nanoTime();
    private int requestsInWindow;

    public ApiKeyAuthenticationFilter(
            String apiKey,
            boolean requireApiKey,
            int maxRequestsPerMinute) {
        String normalizedApiKey = apiKey == null ? "" : apiKey;
        if (requireApiKey && normalizedApiKey.isBlank()) {
            throw new IllegalStateException(
                    "PULSEWATCH_API_KEY es obligatorio cuando PULSEWATCH_REQUIRE_API_KEY=true");
        }
        if (maxRequestsPerMinute < 1) {
            throw new IllegalArgumentException("El límite de solicitudes por minuto debe ser mayor que cero");
        }
        this.configuredApiKey = normalizedApiKey.getBytes(StandardCharsets.UTF_8);
        this.apiKeyConfigured = !normalizedApiKey.isBlank();
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return !path.equals("/api/v1") && !path.startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (apiKeyConfigured) {
            String suppliedApiKey = request.getHeader(API_KEY_HEADER);
            if (!matchesApiKey(suppliedApiKey)) {
                writeProblem(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized", "Se requiere una API key válida en el encabezado X-API-Key.");
                return;
            }
            authenticate("pulsewatch-api-key");
        } else {
            authenticate("local-development");
        }

        long retryAfterSeconds = consumeRateLimit();
        if (retryAfterSeconds > 0) {
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
            writeProblem(response, 429, "Too Many Requests",
                    "Se excedió el límite de solicitudes para esta instancia.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean matchesApiKey(String suppliedApiKey) {
        return suppliedApiKey != null
                && MessageDigest.isEqual(
                        configuredApiKey,
                        suppliedApiKey.getBytes(StandardCharsets.UTF_8));
    }

    private void authenticate(String principal) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))));
        SecurityContextHolder.setContext(context);
    }

    private synchronized long consumeRateLimit() {
        long now = System.nanoTime();
        long elapsed = now - windowStartedAtNanos;
        if (elapsed >= RATE_WINDOW_NANOS) {
            windowStartedAtNanos = now;
            requestsInWindow = 0;
        }

        requestsInWindow++;
        if (requestsInWindow <= maxRequestsPerMinute) {
            return 0;
        }

        long remainingNanos = RATE_WINDOW_NANOS - (System.nanoTime() - windowStartedAtNanos);
        return Math.max(1, (remainingNanos + 999_999_999L) / 1_000_000_000L);
    }

    private void writeProblem(
            HttpServletResponse response,
            int status,
            String title,
            String detail) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\""
                + title + "\",\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}
