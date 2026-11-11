package com.renderia.renderia_backend.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.renderia.renderia_backend.exception.ApiException;

class InputValidatorTests {

    @Test
    void trimsRequiredText() {
        assertEquals("Casa Pasto", InputValidator.requiredText("  Casa Pasto ", "El nombre", 150));
    }

    @Test
    void rejectsEmptyRequiredText() {
        assertThrows(ApiException.class, () -> InputValidator.requiredText("   ", "El nombre", 150));
    }

    @Test
    void rejectsTextLongerThanTheColumn() {
        String tooLong = "a".repeat(151);
        assertThrows(ApiException.class, () -> InputValidator.requiredText(tooLong, "El nombre", 150));
    }

    @Test
    void acceptsNamesWithNumbersAndSymbols() {
        assertEquals("4", InputValidator.requiredText("4", "El nombre", 120));
        assertEquals("María-José O'Neil", InputValidator.requiredText("María-José O'Neil", "El nombre", 120));
    }

    @Test
    void storesBlankOptionalTextAsNull() {
        assertNull(InputValidator.optionalText("  ", "La ubicación", 150));
    }
}
