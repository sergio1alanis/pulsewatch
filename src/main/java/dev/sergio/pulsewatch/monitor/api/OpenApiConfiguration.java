package dev.sergio.pulsewatch.monitor.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "PulseWatchApiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-Key",
        description = "Configura PULSEWATCH_API_KEY y copia su valor aquí.")
@OpenAPIDefinition(
        info = @Info(
                title = "PulseWatch API",
                version = "1.0.0",
                description = "API para registrar monitores HTTP, consultar comprobaciones e incidencias."),
        tags = @Tag(name = "Monitores", description = "Gestión de monitores y sus comprobaciones"))
public class OpenApiConfiguration {
}
