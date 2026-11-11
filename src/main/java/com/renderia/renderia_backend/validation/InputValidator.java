package com.renderia.renderia_backend.validation;

import com.renderia.renderia_backend.exception.ApiException;

/**
 * Reglas comunes para los textos que llegan en las peticiones: quitar espacios
 * sobrantes y no aceptar más caracteres de los que caben en la base de datos.
 * Así un texto enorme pegado en un formulario responde 400 en vez de un error 500.
 */
public final class InputValidator {

    private InputValidator() {
    }

    /** Texto obligatorio: no puede estar vacío ni pasar de maxLength caracteres. */
    public static String requiredText(String value, String fieldLabel, int maxLength) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            throw ApiException.badRequest(fieldLabel + " es obligatorio.");
        }
        return checkLength(clean, fieldLabel, maxLength);
    }

    /** Texto opcional: si viene vacío se guarda como null. */
    public static String optionalText(String value, String fieldLabel, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return checkLength(value.trim(), fieldLabel, maxLength);
    }

    private static String checkLength(String value, String fieldLabel, int maxLength) {
        if (value.length() > maxLength) {
            throw ApiException.badRequest(fieldLabel + " admite máximo " + maxLength + " caracteres.");
        }
        return value;
    }
}
