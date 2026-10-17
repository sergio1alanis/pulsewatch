package dev.sergio.pulsewatch.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class ApiKeyAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsRequestWithoutConfiguredApiKey() throws Exception {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter("local-test-key", true, 10);
        MockHttpServletRequest request = apiRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainContinued = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainContinued.set(true));

        assertEquals(401, response.getStatus());
        assertFalse(chainContinued.get());
    }

    @Test
    void acceptsConfiguredApiKeyAndContinuesChain() throws Exception {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter("local-test-key", true, 10);
        MockHttpServletRequest request = apiRequest();
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, "local-test-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainContinued = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainContinued.set(true));

        assertEquals(200, response.getStatus());
        assertTrue(chainContinued.get());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void limitsAcceptedApiRequestsAndSetsRetryAfter() throws Exception {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter("", false, 2);
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        MockHttpServletResponse thirdResponse = new MockHttpServletResponse();

        filter.doFilter(apiRequest(), firstResponse, (request, response) -> { });
        filter.doFilter(apiRequest(), secondResponse, (request, response) -> { });
        filter.doFilter(apiRequest(), thirdResponse, (request, response) -> { });

        assertEquals(200, firstResponse.getStatus());
        assertEquals(200, secondResponse.getStatus());
        assertEquals(429, thirdResponse.getStatus());
        assertNotNull(thirdResponse.getHeader("Retry-After"));
    }

    @Test
    void failsFastWhenApiKeyIsRequiredButMissing() {
        assertThrows(
                IllegalStateException.class,
                () -> new ApiKeyAuthenticationFilter("", true, 10));
    }

    private MockHttpServletRequest apiRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/monitors");
        request.setServletPath("/api/v1/monitors");
        return request;
    }
}
