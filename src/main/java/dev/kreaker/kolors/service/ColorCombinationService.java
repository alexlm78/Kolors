package dev.kreaker.kolors.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.kreaker.kolors.ColorCombination;
import dev.kreaker.kolors.ColorCombinationForm;
import dev.kreaker.kolors.ColorCombinationRepository;
import dev.kreaker.kolors.ColorForm;
import dev.kreaker.kolors.ColorInCombination;
import dev.kreaker.kolors.ColorInCombinationRepository;
import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.exception.InvalidColorFormatException;

/**
 * Business service for color combination management
 * Provides CRUD operations and data validation
 */
@Service
@Transactional
public class ColorCombinationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ColorCombinationService.class);
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^[0-9A-Fa-f]{6}$");
    
    private final ColorCombinationRepository colorCombinationRepository;
    private final ColorInCombinationRepository colorInCombinationRepository;
    
    @Autowired
    public ColorCombinationService(ColorCombinationRepository colorCombinationRepository,
                                 ColorInCombinationRepository colorInCombinationRepository) {
        this.colorCombinationRepository = colorCombinationRepository;
        this.colorInCombinationRepository = colorInCombinationRepository;
    }
    
    /**
     * Creates a new color combination
     */
    public ColorCombination createCombination(ColorCombinationForm form) {
        logger.info("Creating new color combination: {}", form.getName());
        
        // Validate the form
        validateForm(form);
        
        // Create the entity
        ColorCombination combination = new ColorCombination(form.getName(), form.getColorCount());
        
        // Add colors
        if (form.getColors() != null) {
            for (ColorForm colorForm : form.getColors()) {
                ColorInCombination color = new ColorInCombination(
                    colorForm.getHexValue().toUpperCase(), 
                    colorForm.getPosition()
                );
                combination.addColor(color);
            }
        }
        
        // Save to database
        ColorCombination savedCombination = colorCombinationRepository.save(combination);
        logger.info("Color combination created successfully with ID: {}", savedCombination.getId());
        
        return savedCombination;
    }
    
    /**
     * Gets all color combinations
     */
    @Transactional(readOnly = true)
    public List<ColorCombination> findAllCombinations() {
        logger.debug("Getting all color combinations");
        return colorCombinationRepository.findAllByOrderByCreatedAtDesc();
    }
    
    /**
     * Gets all combinations with pagination
     */
    @Transactional(readOnly = true)
    public Page<ColorCombination> findAllCombinations(Pageable pageable) {
        logger.debug("Getting combinations with pagination: {}", pageable);
        return colorCombinationRepository.findAllByOrderByNameAsc(pageable);
    }
    
    /**
     * Searches combinations by specific criteria
     */
    @Transactional(readOnly = true)
    public List<ColorCombination> searchCombinations(String searchTerm, Integer colorCount) {
        logger.debug("Searching combinations - term: '{}', color count: {}", searchTerm, colorCount);
        
        if (searchTerm != null && !searchTerm.trim().isEmpty() && colorCount != null) {
            return colorCombinationRepository.findSimilarCombinations(searchTerm.trim(), colorCount);
        } else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return colorCombinationRepository.findByNameContainingIgnoreCase(searchTerm.trim());
        } else if (colorCount != null) {
            return colorCombinationRepository.findByColorCount(colorCount);
        } else {
            return findAllCombinations();
        }
    }
    
    /**
     * Searches combinations containing a specific color
     */
    @Transactional(readOnly = true)
    public List<ColorCombination> findByHexValue(String hexValue) {
        logger.debug("Searching combinations containing color: {}", hexValue);
        
        if (!isValidHexColor(hexValue)) {
            throw InvalidColorFormatException.forHexValue(hexValue);
        }
        
        return colorCombinationRepository.findByContainingHexValue(hexValue.toUpperCase());
    }
    
    /**
     * Gets a combination by ID
     */
    @Transactional(readOnly = true)
    public Optional<ColorCombination> findById(Long id) {
        logger.debug("Searching combination by ID: {}", id);
        return colorCombinationRepository.findById(id);
    }
    
    /**
     * Gets a combination by ID or throws exception if not found
     */
    @Transactional(readOnly = true)
    public ColorCombination getById(Long id) {
        logger.debug("Getting combination by ID: {}", id);
        return colorCombinationRepository.findById(id)
                .orElseThrow(() -> new ColorCombinationNotFoundException(id));
    }
    
    /**
     * Updates an existing combination
     */
    @Transactional
    public ColorCombination updateCombination(Long id, ColorCombinationForm form) {
        logger.info("Updating combination ID: {} with data: {}", id, form.getName());
        
        // Validate the form
        validateForm(form);
        
        // Get the existing combination
        ColorCombination existingCombination = getById(id);
        
        // Update basic data
        existingCombination.setName(form.getName());
        existingCombination.setColorCount(form.getColorCount());
        
        // Delete existing colors from database first
        colorInCombinationRepository.deleteByCombinationId(existingCombination.getId());
        
        // Clear the in-memory collection
        existingCombination.getColors().clear();
        
        // Add new colors
        if (form.getColors() != null) {
            for (ColorForm colorForm : form.getColors()) {
                ColorInCombination color = new ColorInCombination(
                    colorForm.getHexValue().toUpperCase(), 
                    colorForm.getPosition()
                );
                existingCombination.addColor(color);
            }
        }
        
        // Save changes
        ColorCombination updatedCombination = colorCombinationRepository.save(existingCombination);
        logger.info("Combination updated successfully: {}", updatedCombination.getId());
        
        return updatedCombination;
    }
    
    /**
     * Deletes a color combination
     */
    public void deleteCombination(Long id) {
        logger.info("Deleting combination ID: {}", id);
        
        // Verify it exists
        if (!colorCombinationRepository.existsById(id)) {
            throw new ColorCombinationNotFoundException(id);
        }
        
        // Delete (colors are automatically deleted by cascade)
        colorCombinationRepository.deleteById(id);
        logger.info("Combination deleted successfully: {}", id);
    }
    
    /**
     * Validates if a hexadecimal color is valid
     */
    public boolean isValidHexColor(String hexValue) {
        return hexValue != null && HEX_COLOR_PATTERN.matcher(hexValue).matches();
    }
    
    /**
     * Validates a list of hexadecimal colors
     */
    public boolean validateHexColors(List<String> hexValues) {
        if (hexValues == null || hexValues.isEmpty()) {
            return false;
        }
        
        return hexValues.stream().allMatch(this::isValidHexColor);
    }
    
    /**
     * Validates that the number of colors matches the color list
     */
    public boolean validateColorCount(Integer colorCount, List<ColorForm> colors) {
        if (colorCount == null || colors == null) {
            return false;
        }
        
        if (colorCount < 2 || colorCount > 4) {
            return false;
        }
        
        return colors.size() == colorCount;
    }
    
    /**
     * Converts an entity to form
     */
    @Transactional(readOnly = true)
    public ColorCombinationForm convertToForm(ColorCombination combination) {
        if (combination == null) {
            return null;
        }
        
        return ColorCombinationForm.fromEntity(combination);
    }
    
    /**
     * Checks if a combination with the same name exists
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return colorCombinationRepository.existsByNameIgnoreCase(name);
    }
    
    /**
     * Gets combination statistics
     */
    @Transactional(readOnly = true)
    public CombinationStatistics getStatistics() {
        logger.debug("Getting combination statistics");
        
        long totalCombinations = colorCombinationRepository.count();
        long combinationsWith2Colors = colorCombinationRepository.countByColorCount(2);
        long combinationsWith3Colors = colorCombinationRepository.countByColorCount(3);
        long combinationsWith4Colors = colorCombinationRepository.countByColorCount(4);
        
        return new CombinationStatistics(
            totalCombinations,
            combinationsWith2Colors,
            combinationsWith3Colors,
            combinationsWith4Colors
        );
    }
    
    /**
     * Gets the most recent combinations
     */
    @Transactional(readOnly = true)
    public List<ColorCombination> getRecentCombinations(int limit) {
        logger.debug("Getting {} most recent combinations", limit);
        return colorCombinationRepository.findMostRecent(
            org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }
    
    /**
     * Validates a complete combination form
     */
    private void validateForm(ColorCombinationForm form) {
        List<String> errors = new ArrayList<>();
        
        // Validate name
        if (form.getName() == null || form.getName().trim().length() < 3) {
            errors.add("Name must have at least 3 characters");
        }
        
        // Validate color count
        if (form.getColorCount() == null || form.getColorCount() < 2 || form.getColorCount() > 4) {
            errors.add("Color count must be between 2 and 4");
        }
        
        // Validate color list
        if (form.getColors() == null || form.getColors().isEmpty()) {
            errors.add("At least one color must be provided");
        } else {
            // Validate that color count matches FIRST
            if (form.getColorCount() != null && form.getColors().size() != form.getColorCount()) {
                errors.add("Number of colors provided (" + form.getColors().size() + 
                          ") does not match specified count (" + form.getColorCount() + ")");
            } else {
                // Only validate color format if count matches
                for (int i = 0; i < form.getColors().size(); i++) {
                    ColorForm color = form.getColors().get(i);
                    if (color.getHexValue() == null || !isValidHexColor(color.getHexValue())) {
                        errors.add("Color at position " + (i + 1) + " has invalid hexadecimal format");
                    }
                    if (color.getPosition() == null || color.getPosition() < 1 || color.getPosition() > 4) {
                        errors.add("Color at position " + (i + 1) + " has invalid position");
                    }
                }
                
                // Validate unique positions only if count matches
                List<Integer> positions = form.getColors().stream()
                        .map(ColorForm::getPosition)
                        .filter(pos -> pos != null)
                        .collect(Collectors.toList());
                
                if (positions.size() != positions.stream().distinct().count()) {
                    errors.add("Color positions must be unique");
                }
            }
        }
        
        // Throw exception if there are errors
        if (!errors.isEmpty()) {
            throw new ColorCombinationValidationException(errors);
        }
    }
    
    /**
     * Inner class for statistics
     */
    public static class CombinationStatistics {
        private final long totalCombinations;
        private final long combinationsWith2Colors;
        private final long combinationsWith3Colors;
        private final long combinationsWith4Colors;
        
        public CombinationStatistics(long totalCombinations, long combinationsWith2Colors, 
                                   long combinationsWith3Colors, long combinationsWith4Colors) {
            this.totalCombinations = totalCombinations;
            this.combinationsWith2Colors = combinationsWith2Colors;
            this.combinationsWith3Colors = combinationsWith3Colors;
            this.combinationsWith4Colors = combinationsWith4Colors;
        }
        
        public long getTotalCombinations() { return totalCombinations; }
        public long getCombinationsWith2Colors() { return combinationsWith2Colors; }
        public long getCombinationsWith3Colors() { return combinationsWith3Colors; }
        public long getCombinationsWith4Colors() { return combinationsWith4Colors; }
    }
}