package dev.kreaker.kolors.exception;

/**
 * Excepción lanzada cuando falla la operación de remover un color de una
 * combinación
 */
public class ColorRemovalException extends RuntimeException {

    public ColorRemovalException(String message) {
        super(message);
    }

    public ColorRemovalException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ColorRemovalException forCombination(Long combinationId, Integer position) {
        return new ColorRemovalException("No se pudo remover el color en la posición " + position
                + " de la combinación con ID: " + combinationId);
    }

    public static ColorRemovalException forInvalidPosition(Integer position) {
        return new ColorRemovalException("Posición inválida para remover color: " + position);
    }

    public static ColorRemovalException forLastColor() {
        return new ColorRemovalException("No se puede remover el último color de la combinación. "
                + "Una combinación debe tener al menos un color");
    }
}
