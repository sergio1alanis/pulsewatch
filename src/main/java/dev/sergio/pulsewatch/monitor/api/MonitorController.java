package dev.sergio.pulsewatch.monitor.api;

import dev.sergio.pulsewatch.monitor.application.MonitorCheckService;
import dev.sergio.pulsewatch.monitor.application.MonitorIncidentService;
import dev.sergio.pulsewatch.monitor.application.MonitorService;
import dev.sergio.pulsewatch.monitor.application.MonitorView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitors")
@Tag(name = "Monitores")
public class MonitorController {

    private final MonitorService monitorService;
    private final MonitorCheckService monitorCheckService;
    private final MonitorIncidentService monitorIncidentService;

    public MonitorController(
            MonitorService monitorService,
            MonitorCheckService monitorCheckService,
            MonitorIncidentService monitorIncidentService) {
        this.monitorService = monitorService;
        this.monitorCheckService = monitorCheckService;
        this.monitorIncidentService = monitorIncidentService;
    }

    @GetMapping
    @Operation(summary = "Listar monitores", description = "Devuelve los monitores ordenados por nombre.")
    public List<MonitorResponse> list() {
        return monitorService.list().stream()
                .map(MonitorResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar monitor")
    public MonitorResponse findById(@PathVariable UUID id) {
        return MonitorResponse.from(monitorService.findById(id));
    }

    @PostMapping("/{id}/checks")
    @Operation(summary = "Ejecutar comprobación inmediata")
    public MonitorResponse checkNow(@PathVariable UUID id) {
        return MonitorResponse.from(monitorCheckService.checkNow(id));
    }

    @GetMapping("/{id}/checks")
    @Operation(summary = "Consultar historial de comprobaciones")
    public List<MonitorCheckResponse> history(@PathVariable UUID id) {
        return monitorCheckService.history(id).stream()
                .map(MonitorCheckResponse::from)
                .toList();
    }

    @GetMapping("/{id}/incidents")
    @Operation(summary = "Consultar incidencias del monitor")
    public List<MonitorIncidentResponse> incidents(@PathVariable UUID id) {
        return monitorIncidentService.history(id).stream()
                .map(MonitorIncidentResponse::from)
                .toList();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Pausar o reactivar un monitor")
    public MonitorResponse setEnabled(
            @PathVariable UUID id,
            @Valid @RequestBody SetMonitorEnabledRequest request) {
        return MonitorResponse.from(monitorService.setEnabled(id, request.enabled()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar configuración del monitor")
    public MonitorResponse updateDetails(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMonitorRequest request) {
        return MonitorResponse.from(monitorService.updateDetails(
                id, request.name(), request.endpoint(), request.intervalSeconds()));
    }

    @PostMapping
    @Operation(summary = "Registrar un monitor")
    public ResponseEntity<MonitorResponse> register(
            @Valid @RequestBody CreateMonitorRequest request) {
        MonitorView monitor = monitorService.register(
                request.name(), request.endpoint(), request.intervalSeconds());
        MonitorResponse response = MonitorResponse.from(monitor);
        URI location = URI.create("/api/v1/monitors/" + response.id());

        return ResponseEntity.created(location).body(response);
    }
}
