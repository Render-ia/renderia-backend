package com.renderia.renderia_backend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void keepsTheStatusAndMessageOfApiExceptions() {
        ResponseEntity<Map<String, String>> response = handler.handleApi(ApiException.notFound("No existe."));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("No existe.", response.getBody().get("message"));
    }

    @Test
    void databaseConflictsBecome409() {
        ResponseEntity<Map<String, String>> response = handler.handleIntegrity(
                new DataIntegrityViolationException("violates foreign key constraint \"fk\""));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void unexpectedErrorsDoNotRevealInternalDetails() throws Exception {
        ResponseEntity<Map<String, String>> response = handler.handleUnexpected(
                new IllegalStateException("SELECT * FROM users WHERE password_hash = ..."));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = response.getBody().get("message");
        assertFalse(message.contains("SELECT"));
        assertFalse(message.contains("IllegalStateException"));
    }
}
