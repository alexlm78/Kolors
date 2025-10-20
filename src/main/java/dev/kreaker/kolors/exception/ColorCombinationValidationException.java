package dev.kreaker.kolors.exception;

import java.util.List;

/** Exception thrown when color combination validation fails */
public class ColorCombinationValidationException extends RuntimeException {

    private final List<String> validationErrors;

    public ColorCombinationValidationException(String message) {
        super(message);
        this.validationErrors = List.of(message);
    }

    public ColorCombinationValidationException(String message, Throwable cause) {
        super(message, cause);
        this.validationErrors = List.of(message);
    }

    public ColorCombinationValidationException(List<String> validationErrors) {
        super("Validation errors: " + String.join(", ", validationErrors));
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
