package com.renderia.renderia_backend.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Convierte las excepciones en respuestas { "message": "..." } con el código HTTP adecuado,
 * que es el formato que espera el frontend (docs/API.md).
 * Los errores inesperados nunca muestran detalles internos (clases, SQL): quedan solo en el log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException e) {
        return message(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException e) {
        return message(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido.");
    }

    /** Por ejemplo /api/v1/projects/abc: el id debe ser un número. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return message(HttpStatus.BAD_REQUEST, "El valor de \"" + e.getName() + "\" no es válido.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleTooLarge(MaxUploadSizeExceededException e) {
        return message(HttpStatus.CONTENT_TOO_LARGE, "El archivo pesa más de 10 MB.");
    }

    /** Datos que la base de datos rechaza: un registro en uso, un valor repetido, etc. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrity(DataIntegrityViolationException e) {
        log.warn("La base de datos rechazó la operación: {}", e.getMostSpecificCause().getMessage());
        return message(HttpStatus.CONFLICT,
                "No se puede completar la operación porque el registro está relacionado con otros datos.");
    }

    /** Cualquier otro error: queda en el log con el detalle y el usuario recibe un mensaje claro. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) throws Exception {
        if (e instanceof org.springframework.web.ErrorResponse || e instanceof org.springframework.security.access.AccessDeniedException
                || e instanceof org.springframework.security.core.AuthenticationException) {
            throw e;
        }
        log.error("Error inesperado", e);
        return message(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado en el servidor. Inténtalo de nuevo en un momento.");
    }

    private static ResponseEntity<Map<String, String>> message(HttpStatus status, String text) {
        return ResponseEntity.status(status).body(Map.of("message", text));
    }
}
