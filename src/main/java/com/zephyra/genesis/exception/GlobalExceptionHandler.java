package com.zephyra.genesis.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String MENSAJE_GENERICO = "No se pudo completar la operación. Intentá nuevamente.";

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "No tenés permisos para realizar esta acción.");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleBusinessException(RuntimeException ex) {
        String mensaje = ex.getMessage() == null || ex.getMessage().isBlank() ? MENSAJE_GENERICO : ex.getMessage();
        return build(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(Exception ex) {
        log.warn("Solicitud inválida: {}", ex.getClass().getSimpleName());
        return build(HttpStatus.BAD_REQUEST, "La solicitud enviada no es válida.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos", ex);
        return build(HttpStatus.CONFLICT, "La operación no es válida porque afecta registros relacionados.");
    }

    // Los errores inesperados se registran completos en el servidor pero nunca se devuelven al cliente.
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex) {
        log.error("Error de acceso a datos", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, MENSAJE_GENERICO);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Error inesperado procesando la solicitud", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, MENSAJE_GENERICO);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of(
                "ok", false,
                "msg", mensaje,
                "mensaje", mensaje
        ));
    }
}
