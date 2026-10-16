package dev.sergio.pulsewatch.monitor.api;

import dev.sergio.pulsewatch.monitor.application.MonitorNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MonitorNotFoundException.class)
    public ProblemDetail handleMonitorNotFound(MonitorNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "No existe un monitor con ese ID");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidMonitor(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "El monitor contiene datos inválidos");
    }
}
