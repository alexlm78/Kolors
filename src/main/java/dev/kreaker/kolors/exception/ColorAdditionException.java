package dev.kreaker.kolors.exception;

/** Excepción lanzada cuando falla la operación de agregar un color a una combinación */
public class ColorAdditionException extends RuntimeException {

    public ColorAdditionException(String message) {
        super(message);
    }

    public ColorAdditionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ColorAdditionException forCombination(Long combinationId, String hexValue) {
        return new ColorAdditionException(
                "No se pudo agregar el color '"
                        + hexValue
                        + "' a la combinación con ID: "
                        + combinationId);
    }

    public static ColorAdditionException forInvalidPosition(Integer position) {
        return new ColorAdditionException("Posición inválida para agregar color: " + position);
    }

    public static ColorAdditionException forDuplicateColor(String hexValue) {
        return new ColorAdditionException(
                "El color '" + hexValue + "' ya existe en esta combinación");
    }
}
