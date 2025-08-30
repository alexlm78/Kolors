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
 * Global exception handling for the application
 * Provides centralized error handling with user-friendly messages
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    private final ColorCombinationService colorCombinationService;
    
    public GlobalExceptionHandler(ColorCombinationService colorCombinationService) {
        this.colorCombinationService = colorCombinationService;
    }
    
    /**
     * Handles exceptions when a color combination is not found
     */
    @ExceptionHandler(ColorCombinationNotFoundException.class)
    public String handleColorCombinationNotFound(ColorCombinationNotFoundException e, 
                                                Model model, 
                                                RedirectAttributes redirectAttributes) {
        logger.warn("Color combination not found: {}", e.getMessage());
        
        redirectAttributes.addFlashAttribute("error", "Color combination not found");
        return "redirect:/combinations/";
    }
    
    /**
     * Handles color combination validation exceptions
     */
    @ExceptionHandler(ColorCombinationValidationException.class)
    public String handleColorCombinationValidation(ColorCombinationValidationException e, 
                                                  Model model) {
        logger.warn("Validation error in color combination: {}", e.getMessage());
        
        // Add necessary data to display the page
        try {
            List<ColorCombination> combinations = colorCombinationService.findAllCombinations();
            ColorCombinationService.CombinationStatistics stats = colorCombinationService.getStatistics();
            
            model.addAttribute("combinations", combinations);
            model.addAttribute("statistics", stats);
            model.addAttribute("totalCombinations", combinations.size());
            model.addAttribute("combinationForm", new ColorCombinationForm());
            
        } catch (Exception ex) {
            logger.error("Error loading data for error page", ex);
            model.addAttribute("combinations", List.of());
            model.addAttribute("combinationForm", new ColorCombinationForm());
        }
        
        // Add validation errors
        List<String> validationErrors = e.getValidationErrors();
        model.addAttribute("error", "Validation errors: " + String.join(", ", validationErrors));
        model.addAttribute("validationErrors", validationErrors);
        
        return "combinations/index";
    }
    
    /**
     * Handles invalid color format exceptions
     */
    @ExceptionHandler(InvalidColorFormatException.class)
    public String handleInvalidColorFormat(InvalidColorFormatException e, 
                                         Model model, 
                                         RedirectAttributes redirectAttributes) {
        logger.warn("Invalid color format: {}", e.getMessage());
        
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/combinations/";
    }
    
    /**
     * Handles general validation exceptions (Bean Validation)
     */
    @ExceptionHandler(ValidationException.class)
    public String handleValidation(ValidationException e, Model model) {
        logger.warn("Validation error: {}", e.getMessage());
        
        // Add necessary data to display the page
        try {
            List<ColorCombination> combinations = colorCombinationService.findAllCombinations();
            ColorCombinationService.CombinationStatistics stats = colorCombinationService.getStatistics();
            
            model.addAttribute("combinations", combinations);
            model.addAttribute("statistics", stats);
            model.addAttribute("totalCombinations", combinations.size());
            model.addAttribute("combinationForm", new ColorCombinationForm());
            
        } catch (Exception ex) {
            logger.error("Error loading data for error page", ex);
            model.addAttribute("combinations", List.of());
            model.addAttribute("combinationForm", new ColorCombinationForm());
        }
        
        model.addAttribute("error", "Validation error: " + e.getMessage());
        return "combinations/index";
    }
    
    /**
     * Handles database and persistence exceptions
     */
    @ExceptionHandler({org.springframework.dao.DataAccessException.class, 
                      org.springframework.transaction.TransactionException.class})
    public String handleDatabaseError(Exception e, Model model, RedirectAttributes redirectAttributes) {
        logger.error("Database error", e);
        
        redirectAttributes.addFlashAttribute("error", 
            "Database error. Please try again later.");
        return "redirect:/combinations/";
    }
    
    /**
     * Handles illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e, 
                                       Model model, 
                                       RedirectAttributes redirectAttributes) {
        logger.warn("Illegal argument: {}", e.getMessage());
        
        redirectAttributes.addFlashAttribute("error", 
            "Invalid data provided: " + e.getMessage());
        return "redirect:/combinations/";
    }
    
    /**
     * Handles all other non-specific exceptions
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericError(Exception e, Model model, RedirectAttributes redirectAttributes) {
        logger.error("Unexpected error in the application", e);
        
        redirectAttributes.addFlashAttribute("error", 
            "An unexpected error has occurred. Please try again.");
        return "redirect:/combinations/";
    }
    
    /**
     * Handles specific runtime errors
     */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeError(RuntimeException e, Model model, RedirectAttributes redirectAttributes) {
        logger.error("Runtime error", e);
        
        // If it's a specific exception we already handle, don't process it here
        if (e instanceof ColorCombinationNotFoundException || 
            e instanceof ColorCombinationValidationException ||
            e instanceof InvalidColorFormatException) {
            throw e; // Re-throw to be handled by specific handler
        }
        
        redirectAttributes.addFlashAttribute("error", 
            "System error. Please contact the administrator if the problem persists.");
        return "redirect:/combinations/";
    }
}