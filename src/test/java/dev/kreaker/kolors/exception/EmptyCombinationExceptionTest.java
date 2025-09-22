package dev.kreaker.kolors.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests unitarios para EmptyCombinationException */
@DisplayName("EmptyCombinationException Unit Tests")
class EmptyCombinationExceptionTest {

  @Test
  @DisplayName("Should create exception with message")
  void shouldCreateExceptionWithMessage() {
    // Given
    String message = "Test error message";

    // When
    EmptyCombinationException exception = new EmptyCombinationException(message);

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
    EmptyCombinationException exception = new EmptyCombinationException(message, cause);

    // Then
    assertEquals(message, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  @DisplayName("Should create exception for creation")
  void shouldCreateExceptionForCreation() {
    // When
    EmptyCombinationException exception = EmptyCombinationException.forCreation();

    // Then
    assertNotNull(exception.getMessage());
    assertTrue(exception.getMessage().contains("crear una combinación sin colores"));
    assertTrue(exception.getMessage().contains("al menos un color"));
  }

  @Test
  @DisplayName("Should create exception for update")
  void shouldCreateExceptionForUpdate() {
    // Given
    Long combinationId = 1L;

    // When
    EmptyCombinationException exception = EmptyCombinationException.forUpdate(combinationId);

    // Then
    assertNotNull(exception.getMessage());
    assertTrue(exception.getMessage().contains(combinationId.toString()));
    assertTrue(exception.getMessage().contains("actualizar la combinación"));
    assertTrue(exception.getMessage().contains("sin colores"));
  }

  @Test
  @DisplayName("Should create exception for removal")
  void shouldCreateExceptionForRemoval() {
    // Given
    Long combinationId = 1L;

    // When
    EmptyCombinationException exception = EmptyCombinationException.forRemoval(combinationId);

    // Then
    assertNotNull(exception.getMessage());
    assertTrue(exception.getMessage().contains(combinationId.toString()));
    assertTrue(exception.getMessage().contains("remover todos los colores"));
    assertTrue(exception.getMessage().contains("al menos un color"));
  }
}
