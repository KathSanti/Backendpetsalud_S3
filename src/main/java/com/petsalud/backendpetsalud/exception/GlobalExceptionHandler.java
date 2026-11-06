package com.petsalud.backendpetsalud.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import com.petsalud.backendpetsalud.dto.RespuestaError;
import com.petsalud.backendpetsalud.monitoreo.SuspiciousActivityService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@AllArgsConstructor 
public class GlobalExceptionHandler {

    private final SuspiciousActivityService monitoreo;

    // 400: falló una validación (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, detalle, req);
    }

    // 400: JSON mal formado o parámetro del tipo equivocado
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, DateTimeParseException.class})
    public ResponseEntity<RespuestaError> manejarFormato(Exception ex, HttpServletRequest req) {
        return respuesta(HttpStatus.BAD_REQUEST, "La solicitud tiene un formato inválido", req);
    }

    // 400: errores de negocio lanzados por los services
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespuestaError> manejarNegocio(IllegalArgumentException ex, HttpServletRequest req) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    // 403: tiene token válido, pero no el rol
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespuestaError> manejarAccesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        String usuario = req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "anónimo";
        monitoreo.registrarAccesoDenegado(usuario, req.getMethod(), req.getRequestURI(), req.getRemoteAddr());
        return respuesta(HttpStatus.FORBIDDEN, "No tienes permisos para acceder a este recurso", req);
    } 

    // Errores lanzados con ResponseStatusException (por ejemplo, el 401 del login)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespuestaError> manejarResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        return respuesta(ex.getStatusCode(), ex.getReason(), req);
    }

    // Todo lo demás
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> manejarGeneral(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse errorSpring) {
            HttpStatusCode status = errorSpring.getStatusCode();
            HttpStatus conocido = HttpStatus.resolve(status.value());
            String mensaje = conocido != null ? conocido.getReasonPhrase() : "Error en la solicitud";
            return respuesta(status, mensaje, req);
        }
        log.error("Error inesperado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno. Intenta más tarde.", req);
    }


    private ResponseEntity<RespuestaError> respuesta(HttpStatusCode status, String mensaje, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(new RespuestaError(status.value(), mensaje, req.getRequestURI(), LocalDateTime.now()));
    }

   
}