package dev.kreaker.kolors;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.service.ColorCombinationService;
import jakarta.validation.Valid;

/**
 * Main controller for color combination management Handles all CRUD operations
 * and combination search
 */
@Controller
@RequestMapping("/combinations")
public class ColorCombinationController {

    private static final Logger logger = LoggerFactory.getLogger(ColorCombinationController.class);
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ColorCombinationService colorCombinationService;

    public ColorCombinationController(ColorCombinationService colorCombinationService) {
        this.colorCombinationService = colorCombinationService;
    }

    /**
     * Main page - lists all combinations with search and filtering
     */
    @GetMapping({"", "/"})
    public String index(Model model,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer colorCount,
            @RequestParam(required = false) Integer minColors,
            @RequestParam(required = false) Integer maxColors,
            @RequestParam(required = false) String hexValue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.debug("Accessing main page - search: '{}', colors: {}, minColors: {}, maxColors: {}, hex: '{}', page: {}",
                search, colorCount, minColors, maxColors, hexValue, page);

        try {
            List<ColorCombination> combinations;

            // Determine color range from colorCount or explicit min/max
            Integer effectiveMinColors = minColors;
            Integer effectiveMaxColors = maxColors;

            if (colorCount != null) {
                effectiveMinColors = colorCount;
                effectiveMaxColors = colorCount;
            }

            // Apply advanced search filters
            combinations = colorCombinationService.searchWithFilters(search, effectiveMinColors, effectiveMaxColors, hexValue);

            // Add data to model
            model.addAttribute("combinations", combinations);
            model.addAttribute("search", search);
            model.addAttribute("colorCount", colorCount);
            model.addAttribute("minColors", minColors);
            model.addAttribute("maxColors", maxColors);
            model.addAttribute("hexValue", hexValue);
            model.addAttribute("totalCombinations", combinations.size());
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);

            // Add empty form to create new combination
            if (!model.containsAttribute("combinationForm")) {
                model.addAttribute("combinationForm", new ColorCombinationForm());
            }

            return "combinations/index";

        } catch (Exception e) {
            logger.error("Error loading main page", e);
            model.addAttribute("error", "Error loading color combinations");
            model.addAttribute("combinations", List.of());
            model.addAttribute("combinationForm", new ColorCombinationForm());
            return "combinations/index";
        }
    }

    /**
     * Create new color combination
     */
    @PostMapping("/create")
    public String createCombination(@Valid @ModelAttribute("combinationForm") ColorCombinationForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        logger.info("Attempting to create new combination: {}", form.getName());

        try {
            // If there are validation errors, return to form
            if (result.hasErrors()) {
                logger.warn("Validation errors when creating combination: {}", result.getAllErrors());

                // Create detailed error message
                StringBuilder errorMessage = new StringBuilder("Validation errors: ");
                result.getAllErrors().forEach(error -> {
                    errorMessage.append(error.getDefaultMessage()).append("; ");
                });

                return addFormDataAndReturnIndex(model, form, errorMessage.toString());
            }

            // Create the combination
            ColorCombination savedCombination = colorCombinationService.createCombination(form);

            // Success message
            redirectAttributes.addFlashAttribute("success",
                    "Combination '" + savedCombination.getName() + "' created successfully");

            logger.info("Combination created successfully: {} (ID: {})",
                    savedCombination.getName(), savedCombination.getId());

            return "redirect:/combinations/";

        } catch (ColorCombinationValidationException e) {
            logger.warn("Validation error when creating combination: {}", e.getMessage());
            model.addAttribute("error", "Validation errors: " + String.join(", ", e.getValidationErrors()));
            return addFormDataAndReturnIndex(model, form, null);

        } catch (Exception e) {
            logger.error("Unexpected error when creating combination", e);
            model.addAttribute("error", "Unexpected error when creating the combination. Please try again.");
            return addFormDataAndReturnIndex(model, form, null);
        }
    }

    /**
     * Show edit form
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {

        logger.debug("Showing edit form for combination ID: {}", id);

        try {
            ColorCombination combination = colorCombinationService.getById(id);
            ColorCombinationForm form = colorCombinationService.convertToForm(combination);

            model.addAttribute("combinationForm", form);
            model.addAttribute("combination", combination);
            model.addAttribute("isEditing", true);

            return "combinations/edit";

        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combination not found for editing: {}", id);
            redirectAttributes.addFlashAttribute("error", "Combination not found");
            return "redirect:/combinations/";

        } catch (Exception e) {
            logger.error("Error loading edit form for ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Error loading combination for editing");
            return "redirect:/combinations/";
        }
    }

    /**
     * Update existing combination
     */
    @PostMapping("/{id}/update")
    public String updateCombination(@PathVariable Long id,
            @Valid @ModelAttribute("combinationForm") ColorCombinationForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        logger.info("Attempting to update combination ID: {} with name: {}", id, form.getName());

        try {
            // If there are validation errors, return to edit form
            if (result.hasErrors()) {
                logger.warn("Validation errors when updating combination ID {}: {}", id, result.getAllErrors());
                ColorCombination combination = colorCombinationService.getById(id);
                model.addAttribute("combination", combination);
                model.addAttribute("isEditing", true);
                model.addAttribute("error", "Form errors. Please correct the data.");
                return "combinations/edit";
            }

            // Update the combination
            ColorCombination updatedCombination = colorCombinationService.updateCombination(id, form);

            // Success message
            redirectAttributes.addFlashAttribute("success",
                    "Combination '" + updatedCombination.getName() + "' updated successfully");

            logger.info("Combination updated successfully: {} (ID: {})",
                    updatedCombination.getName(), updatedCombination.getId());

            return "redirect:/combinations/";

        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combination not found for update: {}", id);
            redirectAttributes.addFlashAttribute("error", "Combination not found");
            return "redirect:/combinations/";

        } catch (ColorCombinationValidationException e) {
            logger.warn("Validation error when updating combination ID {}: {}", id, e.getMessage());
            try {
                ColorCombination combination = colorCombinationService.getById(id);
                model.addAttribute("combination", combination);
                model.addAttribute("isEditing", true);
                model.addAttribute("error", "Validation errors: " + String.join(", ", e.getValidationErrors()));
                return "combinations/edit";
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("error", "Validation error: " + e.getMessage());
                return "redirect:/combinations/";
            }

        } catch (Exception e) {
            logger.error("Unexpected error when updating combination ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Unexpected error when updating the combination");
            return "redirect:/combinations/";
        }
    }

    /**
     * Show delete confirmation page
     */
    @GetMapping("/{id}/confirm-delete")
    public String confirmDelete(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {

        logger.debug("Showing delete confirmation for combination ID: {}", id);

        try {
            ColorCombination combination = colorCombinationService.getById(id);
            model.addAttribute("combination", combination);
            return "combinations/confirm-delete";

        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combination not found for deletion: {}", id);
            redirectAttributes.addFlashAttribute("error", "Combination not found");
            return "redirect:/combinations/";

        } catch (Exception e) {
            logger.error("Error loading delete confirmation for ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Error loading the combination");
            return "redirect:/combinations/";
        }
    }

    /**
     * Delete color combination
     */
    @PostMapping("/{id}/delete")
    public String deleteCombination(@PathVariable Long id, RedirectAttributes redirectAttributes) {

        logger.info("Attempting to delete combination ID: {}", id);

        try {
            // Get name before deleting for the message
            ColorCombination combination = colorCombinationService.getById(id);
            String combinationName = combination.getName();

            // Delete the combination
            colorCombinationService.deleteCombination(id);

            // Success message
            redirectAttributes.addFlashAttribute("success",
                    "Combination '" + combinationName + "' deleted successfully");

            logger.info("Combination deleted successfully: {} (ID: {})", combinationName, id);

        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combination not found for deletion: {}", id);
            redirectAttributes.addFlashAttribute("error", "Combination not found");

        } catch (Exception e) {
            logger.error("Unexpected error when deleting combination ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Error deleting the combination. Please try again.");
        }

        return "redirect:/combinations/";
    }

    /**
     * View details of a specific combination
     */
    @GetMapping("/{id}")
    public String viewCombination(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {

        logger.debug("Showing combination details for ID: {}", id);

        try {
            ColorCombination combination = colorCombinationService.getById(id);
            model.addAttribute("combination", combination);
            return "combinations/view";

        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combination not found for viewing: {}", id);
            redirectAttributes.addFlashAttribute("error", "Combination not found");
            return "redirect:/combinations/";

        } catch (Exception e) {
            logger.error("Error loading combination details for ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Error loading combination details");
            return "redirect:/combinations/";
        }
    }

    /**
     * Add color to existing combination
     */
    @PostMapping("/{id}/add-color")
    public String addColorToCombination(@PathVariable Long id,
            @Valid @ModelAttribute ColorForm colorForm,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        logger.info("Attempting to add color {} to combination ID: {}", colorForm.getHexValue(), id);

        try {
            // Validate color form
            if (result.hasErrors()) {
                logger.warn("Validation errors when adding color to combination ID {}: {}", id, result.getAllErrors());
                redirectAttributes.addFlashAttribute("error", "Invalid color format. Please use 6-character hexadecimal format.");
                return "redirect:/combinations/" + id + "/edit";
            }

            // Add the color
            ColorCombination updatedCombination = colorCombinationService.addColorToCombination(id, colorForm);

            // Success message
            redirectAttributes.addFlashAttribute("success",
                    "Color #" + colorForm.getHexValue() + " added successfully to '" + updatedCombination.getName() + "'");

            logger.info("Color added successfully to combination ID: {}, new color count: {}",
                    id, updatedCombination.getColorCount());

            return "redirect:/combinations/" + id + "/edit";

        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combination not found when adding color: {}", id);
            redirectAttributes.addFlashAttribute("error", "Combination not found");
            return "redirect:/combinations/";

        } catch (Exception e) {
            logger.error("Error adding color to combination ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Error adding color. Please try again.");
            return "redirect:/combinations/" + id + "/edit";
        }
    }

    /**
     * Remove color from existing combination
     */
    @PostMapping("/{id}/remove-color/{position}")
    public String removeColorFromCombination(@PathVariable Long id,
            @PathVariable Integer position,
            RedirectAttributes redirectAttributes) {

        logger.info("Attempting to remove color at position {} from combination ID: {}", position, id);

        try {
            // Remove the color
            ColorCombination updatedCombination = colorCombinationService.removeColorFromCombination(id, position);

            // Success message
            redirectAttributes.addFlashAttribute("success",
                    "Color at position " + position + " removed successfully from '" + updatedCombination.getName() + "'");

            logger.info("Color removed successfully from combination ID: {}, new color count: {}",
                    id, updatedCombination.getColorCount());

            return "redirect:/combinations/" + id + "/edit";

        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combination not found when removing color: {}", id);
            redirectAttributes.addFlashAttribute("error", "Combination not found");
            return "redirect:/combinations/";

        } catch (ColorCombinationValidationException e) {
            logger.warn("Validation error when removing color from combination ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Cannot remove color: " + e.getMessage());
            return "redirect:/combinations/" + id + "/edit";

        } catch (Exception e) {
            logger.error("Error removing color from combination ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Error removing color. Please try again.");
            return "redirect:/combinations/" + id + "/edit";
        }
    }

    /**
     * API endpoint para búsqueda AJAX (opcional)
     */
    @GetMapping("/search")
    public String searchCombinations(@RequestParam(required = false) String term,
            @RequestParam(required = false) Integer colorCount,
            @RequestParam(required = false) Integer minColors,
            @RequestParam(required = false) Integer maxColors,
            @RequestParam(required = false) String hexValue,
            Model model) {

        logger.debug("AJAX search - term: '{}', colors: {}, minColors: {}, maxColors: {}, hex: '{}'",
                term, colorCount, minColors, maxColors, hexValue);

        try {
            List<ColorCombination> combinations;

            // Determine color range from colorCount or explicit min/max
            Integer effectiveMinColors = minColors;
            Integer effectiveMaxColors = maxColors;

            if (colorCount != null) {
                effectiveMinColors = colorCount;
                effectiveMaxColors = colorCount;
            }

            // Apply advanced search filters
            combinations = colorCombinationService.searchWithFilters(term, effectiveMinColors, effectiveMaxColors, hexValue);

            model.addAttribute("combinations", combinations);
            return "combinations/fragments/combination-list :: combinationList";

        } catch (Exception e) {
            logger.error("Error in AJAX search", e);
            model.addAttribute("combinations", List.of());
            model.addAttribute("error", "Search error");
            return "combinations/fragments/combination-list :: combinationList";
        }
    }

    /**
     * Helper method to add form data and return to main page
     */
    private String addFormDataAndReturnIndex(Model model, ColorCombinationForm form, String errorMessage) {
        try {
            // Add existing combinations
            List<ColorCombination> combinations = colorCombinationService.findAllCombinations();
            model.addAttribute("combinations", combinations);

            // Add form with data
            model.addAttribute("combinationForm", form);
            model.addAttribute("totalCombinations", combinations.size());

            // Add error message if provided
            if (errorMessage != null) {
                model.addAttribute("error", errorMessage);
            }

            return "combinations/index";

        } catch (Exception e) {
            logger.error("Error loading data for form", e);
            model.addAttribute("combinations", List.of());
            model.addAttribute("combinationForm", form != null ? form : new ColorCombinationForm());
            model.addAttribute("error", "Error loading data");
            return "combinations/index";
        }
    }

    /**
     * Paginated search endpoint
     */
    @GetMapping("/paginated")
    public String paginatedSearch(Model model,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer colorCount,
            @RequestParam(required = false) Integer minColors,
            @RequestParam(required = false) Integer maxColors,
            @RequestParam(required = false) String hexValue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.debug("Paginated search - search: '{}', colors: {}, minColors: {}, maxColors: {}, hex: '{}', page: {}, size: {}",
                search, colorCount, minColors, maxColors, hexValue, page, size);

        try {
            // Determine color range from colorCount or explicit min/max
            Integer effectiveMinColors = minColors;
            Integer effectiveMaxColors = maxColors;

            if (colorCount != null) {
                effectiveMinColors = colorCount;
                effectiveMaxColors = colorCount;
            }

            // Create pageable
            Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

            // Apply advanced search filters with pagination
            Page<ColorCombination> combinationPage = colorCombinationService.searchWithFilters(
                    search, effectiveMinColors, effectiveMaxColors, hexValue, pageable);

            // Add data to model
            model.addAttribute("combinations", combinationPage.getContent());
            model.addAttribute("search", search);
            model.addAttribute("colorCount", colorCount);
            model.addAttribute("minColors", minColors);
            model.addAttribute("maxColors", maxColors);
            model.addAttribute("hexValue", hexValue);
            model.addAttribute("totalCombinations", combinationPage.getTotalElements());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", combinationPage.getTotalPages());
            model.addAttribute("pageSize", size);
            model.addAttribute("hasNext", combinationPage.hasNext());
            model.addAttribute("hasPrevious", combinationPage.hasPrevious());

            // Add empty form to create new combination
            if (!model.containsAttribute("combinationForm")) {
                model.addAttribute("combinationForm", new ColorCombinationForm());
            }

            return "combinations/index";

        } catch (Exception e) {
            logger.error("Error in paginated search", e);
            model.addAttribute("error", "Error loading color combinations");
            model.addAttribute("combinations", List.of());
            model.addAttribute("combinationForm", new ColorCombinationForm());
            return "combinations/index";
        }
    }

    /**
     * Controller-specific error handling
     */
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        // Add common attributes that are always available
        model.addAttribute("pageTitle", "Color Combination Management");
        // Dynamic color combinations - no fixed limits
        model.addAttribute("supportsDynamicColors", true);
        model.addAttribute("minColors", 1);
    }
}
