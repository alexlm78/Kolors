package dev.kreaker.kolors.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests unitarios para ColorAdditionException */
@DisplayName("ColorAdditionException Unit Tests")
class ColorAdditionExceptionTest {

    @Test
    @DisplayName("Should create exception with message")
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Test error message";

        // When
        ColorAdditionException exception = new ColorAdditionException(message);

        // Then
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");

        // When
        ColorAdditionException exception = new ColorAdditionException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should create exception for combination")
    void shouldCreateExceptionForCombination() {
        // Given
        Long combinationId = 1L;
        String hexValue = "FF0000";

        // When
        ColorAdditionException exception =
                ColorAdditionException.forCombination(combinationId, hexValue);

        // Then
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains(combinationId.toString()));
        assertTrue(exception.getMessage().contains(hexValue));
        assertTrue(exception.getMessage().contains("No se pudo agregar el color"));
    }

    @Test
    @DisplayName("Should create exception for invalid position")
    void shouldCreateExceptionForInvalidPosition() {
        // Given
        Integer position = -1;

        // When
        ColorAdditionException exception = ColorAdditionException.forInvalidPosition(position);

        // Then
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains(position.toString()));
        assertTrue(exception.getMessage().contains("Posición inválida"));
    }

    @Test
    @DisplayName("Should create exception for duplicate color")
    void shouldCreateExceptionForDuplicateColor() {
        // Given
        String hexValue = "00FF00";

        // When
        ColorAdditionException exception = ColorAdditionException.forDuplicateColor(hexValue);

        // Then
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains(hexValue));
        assertTrue(exception.getMessage().contains("ya existe"));
    }
}
