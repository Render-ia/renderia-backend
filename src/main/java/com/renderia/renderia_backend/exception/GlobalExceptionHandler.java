package com.renderia.renderia_backend.exception;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Convierte las excepciones en respuestas { "message": "..." } con el código HTTP adecuado,
 * que es el formato que espera el frontend (docs/API.md).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException e) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(e.getStatus());
        if (e instanceof TooManyRequestsException limited) {
            response.header(HttpHeaders.RETRY_AFTER, String.valueOf(limited.getRetryAfterSeconds()));
        }
        return response.body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "El cuerpo de la petición no es válido."));
    }

    /**
     * Cualquier otro error: el detalle queda solo en el log, con un código para buscarlo.
     * Al cliente nunca se le envía el nombre ni el mensaje de la excepción, porque pueden
     * revelar tablas, rutas o datos internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) throws Exception {
        if (e instanceof org.springframework.web.ErrorResponse || e instanceof org.springframework.security.access.AccessDeniedException
                || e instanceof org.springframework.security.core.AuthenticationException) {
            throw e;
        }
        String reference = UUID.randomUUID().toString().substring(0, 8);
        log.error("Error inesperado [ref {}]", reference, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Ocurrió un error inesperado. Si se repite, reporta el código " + reference + "."));
    }
}
