package dev.sergio.pulsewatch.monitor.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "PulseWatch API",
                version = "1.0.0",
                description = "API para registrar monitores HTTP, consultar comprobaciones e incidencias."),
        tags = @Tag(name = "Monitores", description = "Gestión de monitores y sus comprobaciones"))
public class OpenApiConfiguration {
}
