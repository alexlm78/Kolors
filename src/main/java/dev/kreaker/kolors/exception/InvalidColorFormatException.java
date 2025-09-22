package dev.kreaker.kolors.exception;

/** Excepción lanzada cuando se proporciona un formato de color hexadecimal inválido */
public class InvalidColorFormatException extends RuntimeException {

  public InvalidColorFormatException(String message) {
    super(message);
  }

  public InvalidColorFormatException(String message, Throwable cause) {
    super(message, cause);
  }

  public static InvalidColorFormatException forHexValue(String hexValue) {
    return new InvalidColorFormatException(
        "Formato de color hexadecimal inválido: '"
            + hexValue
            + "'. Debe ser exactamente 6 caracteres (0-9, A-F)");
  }
}
