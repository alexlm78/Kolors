package dev.kreaker.kolors.exception;

/**
 * Excepción lanzada cuando el número de colores no es válido o no coincide con los colores
 * proporcionados
 */
public class InvalidColorCountException extends RuntimeException {

  public InvalidColorCountException(String message) {
    super(message);
  }

  public InvalidColorCountException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidColorCountException(Integer expected, Integer actual) {
    super("Número de colores inválido. Esperado: " + expected + ", Actual: " + actual);
  }
}
