package dev.kreaker.kolors.exception;

/**
 * Excepción lanzada cuando no se encuentra una combinación de colores específica
 */
public class ColorCombinationNotFoundException extends RuntimeException {
    
    public ColorCombinationNotFoundException(String message) {
        super(message);
    }
    
    public ColorCombinationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ColorCombinationNotFoundException(Long id) {
        super("Combinación de colores no encontrada con ID: " + id);
    }
}