package dev.kreaker.kolors;

import dev.kreaker.kolors.exception.ColorAdditionException;
import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.exception.ColorRemovalException;
import dev.kreaker.kolors.exception.EmptyCombinationException;
import dev.kreaker.kolors.exception.InvalidColorCountException;
import dev.kreaker.kolors.exception.InvalidColorFormatException;
import dev.kreaker.kolors.service.ColorCombinationService;
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handling for the application Provides centralized error handling with
 * user-friendly localized messages
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ColorCombinationService colorCombinationService;
    private final MessageSource messageSource;

    public GlobalExceptionHandler(
            ColorCombinationService colorCombinationService, MessageSource messageSource) {
        this.colorCombinationService = colorCombinationService;
        this.messageSource = messageSource;
    }

    /** Helper method to get localized messages */
    private String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, key, Locale.getDefault());
    }

    /** Helper method to prepare model with necessary data for error pages */
    private void prepareErrorModel(Model model) {
        try {
            List<ColorCombination> combinations = colorCombinationService.findAllCombinations();
            ColorCombinationService.CombinationStatistics stats =
                    colorCombinationService.getStatistics();

            model.addAttribute("combinations", combinations);
            model.addAttribute("statistics", stats);
            model.addAttribute("totalCombinations", combinations.size());
            model.addAttribute("combinationForm", new ColorCombinationForm());

        } catch (Exception ex) {
            logger.error("Error loading data for error page", ex);
            model.addAttribute("combinations", List.of());
            model.addAttribute("combinationForm", new ColorCombinationForm());
        }
    }

    /** Handles exceptions when a color combination is not found */
    @ExceptionHandler(ColorCombinationNotFoundException.class)
    public String handleColorCombinationNotFound(
            ColorCombinationNotFoundException e,
            Model model,
            RedirectAttributes redirectAttributes) {
        logger.warn("Color combination not found: {}", e.getMessage());

        String errorMessage = getMessage("error.combination.notfound");
        redirectAttributes.addFlashAttribute("error", errorMessage);
        return "redirect:/combinations/";
    }

    /** Handles color combination validation exceptions */
    @ExceptionHandler(ColorCombinationValidationException.class)
    public String handleColorCombinationValidation(
            ColorCombinationValidationException e, Model model) {
        logger.warn("Validation error in color combination: {}", e.getMessage());

        prepareErrorModel(model);

        // Add validation errors
        List<String> validationErrors = e.getValidationErrors();
        String errorMessage =
                getMessage("error.validation.multiple", String.join(", ", validationErrors));
        model.addAttribute("error", errorMessage);
        model.addAttribute("validationErrors", validationErrors);

        return "combinations/index";
    }

    /** Handles invalid color format exceptions */
    @ExceptionHandler(InvalidColorFormatException.class)
    public String handleInvalidColorFormat(
            InvalidColorFormatException e, Model model, RedirectAttributes redirectAttributes) {
        logger.warn("Invalid color format: {}", e.getMessage());

        String errorMessage = getMessage("error.color.invalid.hex");
        redirectAttributes.addFlashAttribute("error", errorMessage);
        return "redirect:/combinations/";
    }

    /** Handles invalid color count exceptions */
    @ExceptionHandler(InvalidColorCountException.class)
    public String handleInvalidColorCount(
            InvalidColorCountException e, Model model, RedirectAttributes redirectAttributes) {
        logger.warn("Invalid color count: {}", e.getMessage());

        String errorMessage = getMessage("error.combination.colors.required");
        redirectAttributes.addFlashAttribute("error", errorMessage);
        return "redirect:/combinations/";
    }

    /** Handles color addition exceptions */
    @ExceptionHandler(ColorAdditionException.class)
    public String handleColorAddition(
            ColorAdditionException e, Model model, RedirectAttributes redirectAttributes) {
        logger.warn("Color addition failed: {}", e.getMessage());

        String errorMessage = getMessage("error.combination.color.add.failed");
        redirectAttributes.addFlashAttribute("error", errorMessage);
        return "redirect:/combinations/";
    }

    /** Handles color removal exceptions */
    @ExceptionHandler(ColorRemovalException.class)
    public String handleColorRemoval(
            ColorRemovalException e, Model model, RedirectAttributes redirectAttributes) {
        logger.warn("Color removal failed: {}", e.getMessage());

        String errorMessage = getMessage("error.combination.color.remove.failed");
        redirectAttributes.addFlashAttribute("error", errorMessage);
        return "redirect:/combinations/";
    }

    /** Handles empty combination exceptions */
    @ExceptionHandler(EmptyCombinationException.class)
    public String handleEmptyCombination(
            EmptyCombinationException e, Model model, RedirectAttributes redirectAttributes) {
        logger.warn("Empty combination error: {}", e.getMessage());

        String errorMessage = getMessage("error.combination.colors.empty");
        redirectAttributes.addFlashAttribute("error", errorMessage);
        return "redirect:/combinations/";
    }

    /** Handles general validation exceptions (Bean Validation) */
    @ExceptionHandler(ValidationException.class)
    public String handleValidation(ValidationException e, Model model) {
        logger.warn("Validation error: {}", e.getMessage());

        prepareErrorModel(model);

        String errorMessage = getMessage("error.validation.bean", e.getMessage());
        model.addAttribute("error", errorMessage);
        return "combinations/index";
    }

    /** Handles database and persistence exceptions */
    @ExceptionHandler({
        org.springframework.dao.DataAccessException.class,
        org.springframework.transaction.TransactionException.class
    })
    public String handleDatabaseError(
            Exception e, Model model, RedirectAttributes redirectAttributes) {
        logger.error("Database error", e);

        String errorMessage = getMessage("error.database.generic");
        redirectAttributes.addFlashAttribute("error", errorMessage);
        return "redirect:/combinations/";
    }

    /** Handles illegal argument exceptions */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(
            IllegalArgumentException e, Model model, RedirectAttributes redirectAttributes) {
        logger.warn("Illegal argument: {}", e.getMessage());

        String errorMessage = getMessage("error.server.illegal.argument", e.getMessage());
        redirectAttributes.addFlashAttribute("error", errorMessage);
        return "redirect:/combinations/";
    }

    /** Handles specific runtime errors */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeError(
            RuntimeException e, Model model, RedirectAttributes redirectAttributes) {
        logger.error("Runtime error", e);

        // If it's a specific exception we already handle, don't process it here
        if (e instanceof ColorCombinationNotFoundException
                || e instanceof ColorCombinationValidationException
                || e instanceof InvalidColorFormatException
                || e instanceof InvalidColorCountException
                || e instanceof ColorAdditionException
                || e instanceof ColorRemovalException
                || e instanceof EmptyCombinationException) {
            throw e; // Re-throw to be handled by specific handler
        }

        String errorMessage = getMessage("error.server.runtime");
        redirectAttributes.addFlashAttribute("error", errorMessage);
        return "redirect:/combinations/";
    }

    /** Handles all other non-specific exceptions */
    @ExceptionHandler(Exception.class)
    public String handleGenericError(
            Exception e, Model model, RedirectAttributes redirectAttributes) {
        logger.error("Unexpected error in the application", e);

        String errorMessage = getMessage("error.server.generic");
        redirectAttributes.addFlashAttribute("error", errorMessage);
        return "redirect:/combinations/";
    }
}
