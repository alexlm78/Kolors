package dev.kreaker.kolors.exception;

/** Excepción lanzada cuando se intenta crear o mantener una combinación vacía */
public class EmptyCombinationException extends RuntimeException {

    public EmptyCombinationException(String message) {
        super(message);
    }

    public EmptyCombinationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static EmptyCombinationException forCreation() {
        return new EmptyCombinationException(
                "No se puede crear una combinación sin colores. "
                        + "Debe agregar al menos un color");
    }

    public static EmptyCombinationException forUpdate(Long combinationId) {
        return new EmptyCombinationException(
                "No se puede actualizar la combinación con ID "
                        + combinationId
                        + " dejándola sin colores");
    }

    public static EmptyCombinationException forRemoval(Long combinationId) {
        return new EmptyCombinationException(
                "No se puede remover todos los colores de la combinación con ID "
                        + combinationId
                        + ". Una combinación debe tener al menos un color");
    }
}
