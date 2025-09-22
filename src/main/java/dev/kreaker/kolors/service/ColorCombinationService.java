package dev.kreaker.kolors.service;

import dev.kreaker.kolors.ColorCombination;
import dev.kreaker.kolors.ColorCombinationForm;
import dev.kreaker.kolors.ColorCombinationRepository;
import dev.kreaker.kolors.ColorForm;
import dev.kreaker.kolors.ColorInCombination;
import dev.kreaker.kolors.ColorInCombinationRepository;
import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.exception.InvalidColorFormatException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business service for color combination management Provides CRUD operations and data validation
 */
@Service
@Transactional
public class ColorCombinationService {

  private static final Logger logger = LoggerFactory.getLogger(ColorCombinationService.class);
  private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^[0-9A-Fa-f]{6}$");

  private final ColorCombinationRepository colorCombinationRepository;
  private final ColorInCombinationRepository colorInCombinationRepository;
  private final ColorPositionService colorPositionService;

  public ColorCombinationService(
      ColorCombinationRepository colorCombinationRepository,
      ColorInCombinationRepository colorInCombinationRepository,
      ColorPositionService colorPositionService) {
    this.colorCombinationRepository = colorCombinationRepository;
    this.colorInCombinationRepository = colorInCombinationRepository;
    this.colorPositionService = colorPositionService;
  }

  /** Creates a new color combination */
  public ColorCombination createCombination(ColorCombinationForm form) {
    logger.info("Creating new color combination: {}", form.getName());

    // Validate the form
    validateForm(form);

    // Create the entity
    ColorCombination combination = new ColorCombination(form.getName(), form.getColorCount());

    // Add colors
    if (form.getColors() != null) {
      for (ColorForm colorForm : form.getColors()) {
        ColorInCombination color =
            new ColorInCombination(colorForm.getHexValue().toUpperCase(), colorForm.getPosition());
        combination.addColor(color);
      }
    }

    // Save to database
    ColorCombination savedCombination = colorCombinationRepository.save(combination);
    logger.info("Color combination created successfully with ID: {}", savedCombination.getId());

    return savedCombination;
  }

  /** Gets all color combinations with optimized loading */
  @Transactional(readOnly = true)
  public List<ColorCombination> findAllCombinations() {
    logger.debug("Getting all color combinations with optimized loading");
    // Using @EntityGraph annotation in repository to avoid N+1 queries
    return colorCombinationRepository.findAllByOrderByCreatedAtDesc();
  }

  /** Gets all combinations with pagination */
  @Transactional(readOnly = true)
  public Page<ColorCombination> findAllCombinations(Pageable pageable) {
    logger.debug("Getting combinations with pagination: {}", pageable);
    return colorCombinationRepository.findAllByOrderByNameAsc(pageable);
  }

  /** Searches combinations by specific criteria */
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

  /** Searches combinations containing a specific color */
  @Transactional(readOnly = true)
  public List<ColorCombination> findByHexValue(String hexValue) {
    logger.debug("Searching combinations containing color: {}", hexValue);

    if (!isValidHexColor(hexValue)) {
      throw InvalidColorFormatException.forHexValue(hexValue);
    }

    return colorCombinationRepository.findByContainingHexValue(hexValue.toUpperCase());
  }

  /** Advanced search with multiple criteria */
  @Transactional(readOnly = true)
  public List<ColorCombination> searchWithFilters(
      String name, Integer minColors, Integer maxColors, String hexValue) {
    logger.debug(
        "Advanced search - name: '{}', minColors: {}, maxColors: {}, hexValue: '{}'",
        name,
        minColors,
        maxColors,
        hexValue);

    // If searching by hex value, use specific method
    if (hexValue != null && !hexValue.trim().isEmpty()) {
      if (!isValidHexColor(hexValue)) {
        throw InvalidColorFormatException.forHexValue(hexValue);
      }
      return colorCombinationRepository.findByContainingHexValue(hexValue.toUpperCase());
    }

    // If both name and color range specified
    if (name != null && !name.trim().isEmpty() && minColors != null && maxColors != null) {
      return colorCombinationRepository.findByNameAndColorCountRange(
          name.trim(), minColors, maxColors);
    }

    // If only color range specified
    if (minColors != null && maxColors != null) {
      return colorCombinationRepository.findByColorCountBetweenOrderByCreatedAtDesc(
          minColors, maxColors);
    }

    // If only name specified
    if (name != null && !name.trim().isEmpty()) {
      return colorCombinationRepository.findByNameContainingIgnoreCase(name.trim());
    }

    // Default: return all combinations
    return findAllCombinations();
  }

  /** Advanced search with pagination */
  @Transactional(readOnly = true)
  public Page<ColorCombination> searchWithFilters(
      String name, Integer minColors, Integer maxColors, String hexValue, Pageable pageable) {
    logger.debug(
        "Advanced search with pagination - name: '{}', minColors: {}, maxColors: {}, hexValue: '{}', page: {}",
        name,
        minColors,
        maxColors,
        hexValue,
        pageable);

    // If searching by hex value, use specific method
    if (hexValue != null && !hexValue.trim().isEmpty()) {
      if (!isValidHexColor(hexValue)) {
        throw InvalidColorFormatException.forHexValue(hexValue);
      }
      return colorCombinationRepository.findByContainingHexValueWithPagination(
          hexValue.toUpperCase(), pageable);
    }

    // Use complex query for other filters
    return colorCombinationRepository.findWithFilters(
        name != null && !name.trim().isEmpty() ? name.trim() : null,
        minColors,
        maxColors,
        pageable);
  }

  /** Search combinations by color count range */
  @Transactional(readOnly = true)
  public List<ColorCombination> findByColorCountRange(Integer minColors, Integer maxColors) {
    logger.debug("Searching combinations with color count between {} and {}", minColors, maxColors);

    if (minColors == null || maxColors == null) {
      throw new IllegalArgumentException("Both minColors and maxColors must be specified");
    }

    if (minColors < 1 || maxColors < minColors) {
      throw new IllegalArgumentException("Invalid color count range");
    }

    return colorCombinationRepository.findByColorCountBetweenOrderByCreatedAtDesc(
        minColors, maxColors);
  }

  /** Gets a combination by ID with optimized loading */
  @Transactional(readOnly = true)
  public Optional<ColorCombination> findById(Long id) {
    logger.debug("Searching combination by ID with optimized loading: {}", id);
    return colorCombinationRepository.findByIdWithColors(id);
  }

  /**
   * Gets a combination by ID or throws exception if not found Uses optimized loading to fetch
   * colors in single query
   */
  @Transactional(readOnly = true)
  public ColorCombination getById(Long id) {
    logger.debug("Getting combination by ID with optimized loading: {}", id);
    return colorCombinationRepository
        .findByIdWithColors(id)
        .orElseThrow(() -> new ColorCombinationNotFoundException(id));
  }

  /** Updates an existing combination */
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
        ColorInCombination color =
            new ColorInCombination(colorForm.getHexValue().toUpperCase(), colorForm.getPosition());
        existingCombination.addColor(color);
      }
    }

    // Save changes
    ColorCombination updatedCombination = colorCombinationRepository.save(existingCombination);
    logger.info("Combination updated successfully: {}", updatedCombination.getId());

    return updatedCombination;
  }

  /** Deletes a color combination */
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

  /** Validates if a hexadecimal color is valid */
  public boolean isValidHexColor(String hexValue) {
    return hexValue != null && HEX_COLOR_PATTERN.matcher(hexValue).matches();
  }

  /** Validates a list of hexadecimal colors */
  public boolean validateHexColors(List<String> hexValues) {
    if (hexValues == null || hexValues.isEmpty()) {
      return false;
    }

    return hexValues.stream().allMatch(this::isValidHexColor);
  }

  /** Validates that the number of colors matches the color list */
  public boolean validateColorCount(Integer colorCount, List<ColorForm> colors) {
    if (colorCount == null || colors == null) {
      return false;
    }

    if (colorCount < 2 || colorCount > 4) {
      return false;
    }

    return colors.size() == colorCount;
  }

  /** Converts an entity to form */
  @Transactional(readOnly = true)
  public ColorCombinationForm convertToForm(ColorCombination combination) {
    if (combination == null) {
      return null;
    }

    return ColorCombinationForm.fromEntity(combination);
  }

  /** Checks if a combination with the same name exists */
  @Transactional(readOnly = true)
  public boolean existsByName(String name) {
    return colorCombinationRepository.existsByNameIgnoreCase(name);
  }

  /** Gets combination statistics */
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
        combinationsWith4Colors);
  }

  /** Gets the most recent combinations */
  @Transactional(readOnly = true)
  public List<ColorCombination> getRecentCombinations(int limit) {
    logger.debug("Getting {} most recent combinations", limit);
    return colorCombinationRepository.findMostRecent(
        org.springframework.data.domain.PageRequest.of(0, limit));
  }

  /** Validates a complete combination form */
  private void validateForm(ColorCombinationForm form) {
    List<String> errors = new ArrayList<>();

    // Validate name
    if (form.getName() == null || form.getName().trim().length() < 3) {
      errors.add("Name must have at least 3 characters");
    }

    // Validate color list (must have at least one color)
    if (form.getColors() == null || form.getColors().isEmpty()) {
      errors.add("Must have at least one color");
    } else {
      // Validate each color
      for (int i = 0; i < form.getColors().size(); i++) {
        ColorForm color = form.getColors().get(i);
        if (color.getHexValue() == null || !isValidHexColor(color.getHexValue())) {
          errors.add("Color at position " + (i + 1) + " has invalid hexadecimal format");
        }
        if (color.getPosition() == null || color.getPosition() < 1) {
          errors.add("Color at position " + (i + 1) + " has invalid position");
        }
      }

      // Validate unique positions
      List<Integer> positions =
          form.getColors().stream()
              .map(ColorForm::getPosition)
              .filter(pos -> pos != null)
              .collect(Collectors.toList());

      if (positions.size() != positions.stream().distinct().count()) {
        errors.add("Color positions must be unique");
      }
    }

    // Throw exception if there are errors
    if (!errors.isEmpty()) {
      throw new ColorCombinationValidationException(errors);
    }
  }

  /** Adds a color to an existing combination */
  public ColorCombination addColorToCombination(Long combinationId, ColorForm colorForm) {
    logger.info("Adding color to combination ID: {}", combinationId);

    // Validate inputs
    if (combinationId == null) {
      throw new IllegalArgumentException("Combination ID cannot be null");
    }
    if (colorForm == null || colorForm.getHexValue() == null) {
      throw new IllegalArgumentException("Color form and hex value cannot be null");
    }
    if (!isValidHexColor(colorForm.getHexValue())) {
      throw new InvalidColorFormatException(
          "Invalid hexadecimal color format: " + colorForm.getHexValue());
    }

    // Get the existing combination
    ColorCombination combination =
        colorCombinationRepository
            .findById(combinationId)
            .orElseThrow(
                () ->
                    new ColorCombinationNotFoundException(
                        "Combination not found with ID: " + combinationId));

    // Get the next available position
    Integer nextPosition = combination.getNextAvailablePosition();

    // Create and add the new color
    ColorInCombination newColor =
        new ColorInCombination(colorForm.getHexValue().toUpperCase(), nextPosition);
    combination.addColor(newColor);

    // Save and return
    ColorCombination savedCombination = colorCombinationRepository.save(combination);
    logger.info(
        "Color added successfully to combination ID: {}, new color count: {}",
        combinationId,
        savedCombination.getColorCount());

    return savedCombination;
  }

  /** Removes a color from an existing combination at a specific position */
  public ColorCombination removeColorFromCombination(Long combinationId, Integer position) {
    logger.info("Removing color at position {} from combination ID: {}", position, combinationId);

    // Validate inputs
    if (combinationId == null) {
      throw new IllegalArgumentException("Combination ID cannot be null");
    }
    if (position == null || position < 1) {
      throw new IllegalArgumentException("Position must be a positive integer");
    }

    // Get the existing combination
    ColorCombination combination =
        colorCombinationRepository
            .findById(combinationId)
            .orElseThrow(
                () ->
                    new ColorCombinationNotFoundException(
                        "Combination not found with ID: " + combinationId));

    // Check if combination has more than one color (cannot remove the last color)
    if (combination.getColors().size() <= 1) {
      throw new ColorCombinationValidationException(
          "Cannot remove the last color from a combination");
    }

    // Find and remove the color at the specified position
    boolean colorRemoved =
        combination.getColors().removeIf(color -> color.getPosition().equals(position));

    if (!colorRemoved) {
      throw new ColorCombinationNotFoundException(
          "No color found at position " + position + " in combination " + combinationId);
    }

    // Reorder positions and update color count
    combination.getColors().sort((c1, c2) -> c1.getPosition().compareTo(c2.getPosition()));
    for (int i = 0; i < combination.getColors().size(); i++) {
      combination.getColors().get(i).setPosition(i + 1);
    }
    combination.setColorCount(combination.getColors().size());

    // Save and return
    ColorCombination savedCombination = colorCombinationRepository.save(combination);
    logger.info(
        "Color removed successfully from combination ID: {}, new color count: {}",
        combinationId,
        savedCombination.getColorCount());

    return savedCombination;
  }

  /** Reorders color positions after a color removal to ensure sequential positions */
  public void reorderColorsAfterRemoval(Long combinationId, Integer removedPosition) {
    colorPositionService.reorderPositionsAfterRemoval(combinationId, removedPosition);
  }

  /** Validates that a combination has at least the minimum required colors */
  public boolean validateMinimumColors(List<ColorForm> colors) {
    return colors != null && !colors.isEmpty();
  }

  /** Inner class for statistics */
  public static class CombinationStatistics {

    private final long totalCombinations;
    private final long combinationsWith2Colors;
    private final long combinationsWith3Colors;
    private final long combinationsWith4Colors;

    public CombinationStatistics(
        long totalCombinations,
        long combinationsWith2Colors,
        long combinationsWith3Colors,
        long combinationsWith4Colors) {
      this.totalCombinations = totalCombinations;
      this.combinationsWith2Colors = combinationsWith2Colors;
      this.combinationsWith3Colors = combinationsWith3Colors;
      this.combinationsWith4Colors = combinationsWith4Colors;
    }

    public long getTotalCombinations() {
      return totalCombinations;
    }

    public long getCombinationsWith2Colors() {
      return combinationsWith2Colors;
    }

    public long getCombinationsWith3Colors() {
      return combinationsWith3Colors;
    }

    public long getCombinationsWith4Colors() {
      return combinationsWith4Colors;
    }
  }
}
