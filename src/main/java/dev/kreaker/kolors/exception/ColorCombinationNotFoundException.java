package dev.kreaker.kolors.exception;

/** Exception thrown when a specific color combination is not found */
public class ColorCombinationNotFoundException extends RuntimeException {

  public ColorCombinationNotFoundException(String message) {
    super(message);
  }

  public ColorCombinationNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public ColorCombinationNotFoundException(Long id) {
    super("Color combination not found with ID: " + id);
  }
}
