package dev.kreaker.kolors;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.exception.InvalidColorFormatException;
import dev.kreaker.kolors.service.ColorCombinationService;
import jakarta.validation.Valid;

/**
 * REST API controller for dynamic color combination operations Provides AJAX
 * endpoints for real-time color management
 */
@RestController
@RequestMapping("/api/combinations")
public class ColorCombinationRestController {

    private static final Logger logger = LoggerFactory.getLogger(ColorCombinationRestController.class);

    private final ColorCombinationService colorCombinationService;

    public ColorCombinationRestController(ColorCombinationService colorCombinationService) {
        this.colorCombinationService = colorCombinationService;
    }

    /**
     * Add color to combination via AJAX
     */
    @PostMapping("/{id}/colors")
    public ResponseEntity<Map<String, Object>> addColor(@PathVariable Long id,
            @Valid @RequestBody ColorForm colorForm,
            BindingResult result) {

        logger.info("AJAX request to add color {} to combination ID: {}", colorForm.getHexValue(), id);

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate input
            if (result.hasErrors()) {
                logger.warn("Validation errors when adding color via AJAX: {}", result.getAllErrors());
                response.put("success", false);
                response.put("message", "Invalid color format. Please use 6-character hexadecimal format.");
                return ResponseEntity.badRequest().body(response);
            }

            // Add the color
            ColorCombination updatedCombination = colorCombinationService.addColorToCombination(id, colorForm);

            // Success response
            response.put("success", true);
            response.put("message", "Color added successfully");
            response.put("combination", createCombinationResponse(updatedCombination));

            logger.info("Color added successfully via AJAX to combination ID: {}", id);
            return ResponseEntity.ok(response);

        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combination not found when adding color via AJAX: {}", id);
            response.put("success", false);
            response.put("message", "Combination not found");
            return ResponseEntity.notFound().build();

        } catch (InvalidColorFormatException e) {
            logger.warn("Invalid color format when adding color via AJAX: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Invalid color format: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("Error adding color via AJAX to combination ID: " + id, e);
            response.put("success", false);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Remove color from combination via AJAX
     */
    @DeleteMapping("/{id}/colors/{position}")
    public ResponseEntity<Map<String, Object>> removeColor(@PathVariable Long id,
            @PathVariable Integer position) {

        logger.info("AJAX request to remove color at position {} from combination ID: {}", position, id);

        Map<String, Object> response = new HashMap<>();

        try {
            // Remove the color
            ColorCombination updatedCombination = colorCombinationService.removeColorFromCombination(id, position);

            // Success response
            response.put("success", true);
            response.put("message", "Color removed successfully");
            response.put("combination", createCombinationResponse(updatedCombination));

            logger.info("Color removed successfully via AJAX from combination ID: {}", id);
            return ResponseEntity.ok(response);

        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combination not found when removing color via AJAX: {}", id);
            response.put("success", false);
            response.put("message", "Combination not found");
            return ResponseEntity.notFound().build();

        } catch (ColorCombinationValidationException e) {
            logger.warn("Validation error when removing color via AJAX: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("Error removing color via AJAX from combination ID: " + id, e);
            response.put("success", false);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Validate color format via AJAX
     */
    @PostMapping("/validate-color")
    public ResponseEntity<Map<String, Object>> validateColor(@Valid @RequestBody ColorForm colorForm,
            BindingResult result) {

        logger.debug("AJAX request to validate color: {}", colorForm.getHexValue());

        Map<String, Object> response = new HashMap<>();

        try {
            if (result.hasErrors()) {
                response.put("valid", false);
                response.put("message", "Invalid hexadecimal format");
                return ResponseEntity.ok(response);
            }

            // Additional validation using service
            boolean isValid = colorCombinationService.isValidHexColor(colorForm.getHexValue());

            response.put("valid", isValid);
            response.put("message", isValid ? "Valid color" : "Invalid hexadecimal format");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating color via AJAX", e);
            response.put("valid", false);
            response.put("message", "Validation error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get combination details via AJAX
     */
    @PostMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCombination(@PathVariable Long id) {

        logger.debug("AJAX request to get combination ID: {}", id);

        Map<String, Object> response = new HashMap<>();

        try {
            ColorCombination combination = colorCombinationService.getById(id);

            response.put("success", true);
            response.put("combination", createCombinationResponse(combination));

            return ResponseEntity.ok(response);

        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combination not found via AJAX: {}", id);
            response.put("success", false);
            response.put("message", "Combination not found");
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("Error getting combination via AJAX: " + id, e);
            response.put("success", false);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Helper method to create combination response object
     */
    private Map<String, Object> createCombinationResponse(ColorCombination combination) {
        Map<String, Object> combinationData = new HashMap<>();
        combinationData.put("id", combination.getId());
        combinationData.put("name", combination.getName());
        combinationData.put("colorCount", combination.getColorCount());
        combinationData.put("createdAt", combination.getCreatedAt());

        // Add colors data
        if (combination.getColors() != null) {
            combinationData.put("colors", combination.getColors().stream()
                    .sorted((c1, c2) -> c1.getPosition().compareTo(c2.getPosition()))
                    .map(color -> {
                        Map<String, Object> colorData = new HashMap<>();
                        colorData.put("id", color.getId());
                        colorData.put("hexValue", color.getHexValue());
                        colorData.put("position", color.getPosition());
                        colorData.put("formattedHex", color.getFormattedHex());
                        return colorData;
                    })
                    .toList());
        }

        return combinationData;
    }
}
