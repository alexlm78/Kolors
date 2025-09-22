package dev.kreaker.kolors.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests unitarios para ColorRemovalException */
@DisplayName("ColorRemovalException Unit Tests")
class ColorRemovalExceptionTest {

  @Test
  @DisplayName("Should create exception with message")
  void shouldCreateExceptionWithMessage() {
    // Given
    String message = "Test error message";

    // When
    ColorRemovalException exception = new ColorRemovalException(message);

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
    ColorRemovalException exception = new ColorRemovalException(message, cause);

    // Then
    assertEquals(message, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  @DisplayName("Should create exception for combination")
  void shouldCreateExceptionForCombination() {
    // Given
    Long combinationId = 1L;
    Integer position = 2;

    // When
    ColorRemovalException exception = ColorRemovalException.forCombination(combinationId, position);

    // Then
    assertNotNull(exception.getMessage());
    assertTrue(exception.getMessage().contains(combinationId.toString()));
    assertTrue(exception.getMessage().contains(position.toString()));
    assertTrue(exception.getMessage().contains("No se pudo remover el color"));
  }

  @Test
  @DisplayName("Should create exception for invalid position")
  void shouldCreateExceptionForInvalidPosition() {
    // Given
    Integer position = -1;

    // When
    ColorRemovalException exception = ColorRemovalException.forInvalidPosition(position);

    // Then
    assertNotNull(exception.getMessage());
    assertTrue(exception.getMessage().contains(position.toString()));
    assertTrue(exception.getMessage().contains("Posición inválida"));
  }

  @Test
  @DisplayName("Should create exception for last color")
  void shouldCreateExceptionForLastColor() {
    // When
    ColorRemovalException exception = ColorRemovalException.forLastColor();

    // Then
    assertNotNull(exception.getMessage());
    assertTrue(exception.getMessage().contains("último color"));
    assertTrue(exception.getMessage().contains("al menos un color"));
  }
}
