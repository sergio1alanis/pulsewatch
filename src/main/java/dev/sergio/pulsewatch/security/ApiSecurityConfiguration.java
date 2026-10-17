package dev.sergio.pulsewatch.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class ApiSecurityConfiguration {

    @Bean
    UserDetailsService disabledPasswordAuthentication() {
        return username -> {
            throw new UsernameNotFoundException(
                    "PulseWatch uses API-key authentication, not username/password accounts.");
        };
    }

    @Bean
    SecurityFilterChain pulseWatchSecurityFilterChain(
            HttpSecurity http,
            @Value("${pulsewatch.security.api-key:}") String apiKey,
            @Value("${pulsewatch.security.require-api-key:false}") boolean requireApiKey,
            @Value("${pulsewatch.security.rate-limit-per-minute:120}") int rateLimitPerMinute)
            throws Exception {
        ApiKeyAuthenticationFilter apiKeyFilter = new ApiKeyAuthenticationFilter(
                apiKey, requireApiKey, rateLimitPerMinute);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/error").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(apiKeyFilter, AnonymousAuthenticationFilter.class)
                .build();
    }
}
