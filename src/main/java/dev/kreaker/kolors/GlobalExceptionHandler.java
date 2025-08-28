package dev.kreaker.kolors;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.exception.InvalidColorFormatException;
import dev.kreaker.kolors.service.ColorCombinationService;
import jakarta.validation.ValidationException;

/**
 * Manejo global de excepciones para la aplicación
 * Proporciona manejo centralizado de errores con mensajes amigables al usuario
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    private final ColorCombinationService colorCombinationService;
    
    public GlobalExceptionHandler(ColorCombinationService colorCombinationService) {
        this.colorCombinationService = colorCombinationService;
    }
    
    /**
     * Maneja excepciones cuando no se encuentra una combinación de colores
     */
    @ExceptionHandler(ColorCombinationNotFoundException.class)
    public String handleColorCombinationNotFound(ColorCombinationNotFoundException e, 
                                                Model model, 
                                                RedirectAttributes redirectAttributes) {
        logger.warn("Combinación de colores no encontrada: {}", e.getMessage());
        
        redirectAttributes.addFlashAttribute("error", "Combinación de colores no encontrada");
        return "redirect:/combinations/";
    }
    
    /**
     * Maneja excepciones de validación de combinaciones de colores
     */
    @ExceptionHandler(ColorCombinationValidationException.class)
    public String handleColorCombinationValidation(ColorCombinationValidationException e, 
                                                  Model model) {
        logger.warn("Error de validación en combinación de colores: {}", e.getMessage());
        
        // Agregar datos necesarios para mostrar la página
        try {
            List<ColorCombination> combinations = colorCombinationService.findAllCombinations();
            ColorCombinationService.CombinationStatistics stats = colorCombinationService.getStatistics();
            
            model.addAttribute("combinations", combinations);
            model.addAttribute("statistics", stats);
            model.addAttribute("totalCombinations", combinations.size());
            model.addAttribute("combinationForm", new ColorCombinationForm());
            
        } catch (Exception ex) {
            logger.error("Error al cargar datos para página de error", ex);
            model.addAttribute("combinations", List.of());
            model.addAttribute("combinationForm", new ColorCombinationForm());
        }
        
        // Agregar errores de validación
        List<String> validationErrors = e.getValidationErrors();
        model.addAttribute("error", "Errores de validación: " + String.join(", ", validationErrors));
        model.addAttribute("validationErrors", validationErrors);
        
        return "combinations/index";
    }
    
    /**
     * Maneja excepciones de formato de color inválido
     */
    @ExceptionHandler(InvalidColorFormatException.class)
    public String handleInvalidColorFormat(InvalidColorFormatException e, 
                                         Model model, 
                                         RedirectAttributes redirectAttributes) {
        logger.warn("Formato de color inválido: {}", e.getMessage());
        
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/combinations/";
    }
    
    /**
     * Maneja excepciones de validación general (Bean Validation)
     */
    @ExceptionHandler(ValidationException.class)
    public String handleValidation(ValidationException e, Model model) {
        logger.warn("Error de validación: {}", e.getMessage());
        
        // Agregar datos necesarios para mostrar la página
        try {
            List<ColorCombination> combinations = colorCombinationService.findAllCombinations();
            ColorCombinationService.CombinationStatistics stats = colorCombinationService.getStatistics();
            
            model.addAttribute("combinations", combinations);
            model.addAttribute("statistics", stats);
            model.addAttribute("totalCombinations", combinations.size());
            model.addAttribute("combinationForm", new ColorCombinationForm());
            
        } catch (Exception ex) {
            logger.error("Error al cargar datos para página de error", ex);
            model.addAttribute("combinations", List.of());
            model.addAttribute("combinationForm", new ColorCombinationForm());
        }
        
        model.addAttribute("error", "Error de validación: " + e.getMessage());
        return "combinations/index";
    }
    
    /**
     * Maneja excepciones de base de datos y persistencia
     */
    @ExceptionHandler({org.springframework.dao.DataAccessException.class, 
                      org.springframework.transaction.TransactionException.class})
    public String handleDatabaseError(Exception e, Model model, RedirectAttributes redirectAttributes) {
        logger.error("Error de base de datos", e);
        
        redirectAttributes.addFlashAttribute("error", 
            "Error de base de datos. Por favor, inténtelo de nuevo más tarde.");
        return "redirect:/combinations/";
    }
    
    /**
     * Maneja excepciones de argumentos ilegales
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e, 
                                       Model model, 
                                       RedirectAttributes redirectAttributes) {
        logger.warn("Argumento ilegal: {}", e.getMessage());
        
        redirectAttributes.addFlashAttribute("error", 
            "Datos inválidos proporcionados: " + e.getMessage());
        return "redirect:/combinations/";
    }
    
    /**
     * Maneja todas las demás excepciones no específicas
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericError(Exception e, Model model, RedirectAttributes redirectAttributes) {
        logger.error("Error inesperado en la aplicación", e);
        
        redirectAttributes.addFlashAttribute("error", 
            "Ha ocurrido un error inesperado. Por favor, inténtelo de nuevo.");
        return "redirect:/combinations/";
    }
    
    /**
     * Maneja errores de tiempo de ejecución específicos
     */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeError(RuntimeException e, Model model, RedirectAttributes redirectAttributes) {
        logger.error("Error de tiempo de ejecución", e);
        
        // Si es una excepción específica que ya manejamos, no la procesamos aquí
        if (e instanceof ColorCombinationNotFoundException || 
            e instanceof ColorCombinationValidationException ||
            e instanceof InvalidColorFormatException) {
            throw e; // Re-lanzar para que sea manejada por el handler específico
        }
        
        redirectAttributes.addFlashAttribute("error", 
            "Error del sistema. Por favor, contacte al administrador si el problema persiste.");
        return "redirect:/combinations/";
    }
}