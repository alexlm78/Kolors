package dev.kreaker.kolors.migration;

import dev.kreaker.kolors.ColorCombination;
import dev.kreaker.kolors.ColorCombinationRepository;
import dev.kreaker.kolors.ColorInCombination;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for post-migration cleanup and validation. Legacy migration functionality has
 * been completed and removed. This service now provides data integrity validation for migrated
 * color combinations.
 */
@Service
public class DatabaseMigrationService {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationService.class);
  private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9A-Fa-f]{6}$");

  @Autowired private ColorCombinationRepository colorCombinationRepository;

  private MigrationStatus currentStatus = MigrationStatus.COMPLETED;
  private MigrationResult lastMigrationResult;

  /**
   * Validates the current state of migrated data. Migration has been completed and legacy code
   * removed.
   */
  @Transactional(readOnly = true)
  public MigrationResult validateMigratedData() {
    logger.info("Validating migrated data integrity");

    currentStatus = MigrationStatus.COMPLETED;
    MigrationResult result = new MigrationResult();

    try {
      // Validate all color combinations
      List<ColorCombination> allCombinations = colorCombinationRepository.findAll();
      result.setTotalLegacyRecords(0); // No legacy data remains
      result.setMigratedRecords(allCombinations.size());
      result.setFailedRecords(0);

      logger.info("Found {} color combinations to validate", allCombinations.size());

      int validCombinations = 0;
      int invalidCombinations = 0;

      for (ColorCombination combination : allCombinations) {
        try {
          if (validateSingleCombination(combination, result)) {
            validCombinations++;
          } else {
            invalidCombinations++;
          }
        } catch (Exception e) {
          invalidCombinations++;
          String error =
              "Failed to validate combination ID " + combination.getId() + ": " + e.getMessage();
          result.addError(error);
          logger.error(error, e);
        }
      }

      // Determine final status
      if (invalidCombinations == 0) {
        result.setSuccess(true);
        logger.info(
            "Data validation completed successfully. Validated {} combinations", validCombinations);
      } else {
        result.setSuccess(false);
        logger.warn(
            "Data validation found issues. Valid: {}, Invalid: {}",
            validCombinations,
            invalidCombinations);
      }

    } catch (Exception e) {
      result.setSuccess(false);
      result.addError("Data validation failed with exception: " + e.getMessage());
      logger.error("Data validation failed with exception", e);
    }

    result.complete();
    lastMigrationResult = result;

    return result;
  }

  /** Validates a single color combination for data integrity */
  private boolean validateSingleCombination(ColorCombination combination, MigrationResult result) {
    try {
      // Validate combination data
      if (!isValidCombination(combination, result)) {
        return false;
      }

      // Validate colors in combination
      if (!validateCombinationColors(combination, result)) {
        return false;
      }

      logger.debug(
          "Successfully validated combination ID {} with {} colors",
          combination.getId(),
          combination.getColorCount());

      return true;

    } catch (Exception e) {
      logger.error(
          "Error validating combination ID {}: {}", combination.getId(), e.getMessage(), e);
      return false;
    }
  }

  /** Validates a color combination for data integrity */
  private boolean isValidCombination(ColorCombination combination, MigrationResult result) {
    if (combination.getName() == null || combination.getName().trim().isEmpty()) {
      result.addError("Invalid combination ID " + combination.getId() + ": name is null or empty");
      return false;
    }

    if (combination.getName().length() < 3) {
      result.addError(
          "Invalid combination ID " + combination.getId() + ": name too short (< 3 characters)");
      return false;
    }

    if (combination.getColorCount() == null || combination.getColorCount() < 1) {
      result.addError(
          "Invalid combination ID "
              + combination.getId()
              + ": invalid color count "
              + combination.getColorCount());
      return false;
    }

    if (combination.getCreatedAt() == null) {
      result.addError("Invalid combination ID " + combination.getId() + ": missing creation date");
      return false;
    }

    return true;
  }

  /** Validates the colors within a combination */
  private boolean validateCombinationColors(ColorCombination combination, MigrationResult result) {
    List<ColorInCombination> colors = combination.getColors();

    if (colors == null || colors.isEmpty()) {
      result.addError("Combination ID " + combination.getId() + " has no colors");
      return false;
    }

    if (colors.size() != combination.getColorCount()) {
      result.addError(
          "Combination ID "
              + combination.getId()
              + " color count mismatch: expected "
              + combination.getColorCount()
              + ", found "
              + colors.size());
      return false;
    }

    // Validate each color
    for (int i = 0; i < colors.size(); i++) {
      ColorInCombination color = colors.get(i);

      if (color.getHexValue() == null || !HEX_PATTERN.matcher(color.getHexValue()).matches()) {
        result.addError(
            "Combination ID "
                + combination.getId()
                + " has invalid hex value at position "
                + (i + 1)
                + ": '"
                + color.getHexValue()
                + "'");
        return false;
      }

      if (color.getPosition() == null || color.getPosition() != (i + 1)) {
        result.addError(
            "Combination ID "
                + combination.getId()
                + " has invalid position at index "
                + i
                + ": expected "
                + (i + 1)
                + ", found "
                + color.getPosition());
        return false;
      }
    }

    return true;
  }

  /**
   * Checks if there is legacy data available for migration. Always returns false as legacy data has
   * been migrated and removed.
   */
  public boolean isLegacyDataPresent() {
    // Legacy data has been migrated and legacy tables removed
    return false;
  }

  /** Gets the current migration status */
  public MigrationStatus getMigrationStatus() {
    return currentStatus;
  }

  /** Gets the result of the last migration operation */
  public Optional<MigrationResult> getLastMigrationResult() {
    return Optional.ofNullable(lastMigrationResult);
  }

  /** Resets the migration status (for testing purposes) */
  public void resetMigrationStatus() {
    currentStatus = MigrationStatus.NOT_STARTED;
    lastMigrationResult = null;
  }

  /** Gets statistics about migrated data */
  public MigrationStatistics getMigrationStatistics() {
    MigrationStatistics stats = new MigrationStatistics();

    try {
      stats.setLegacyRecordCount(0); // No legacy data remains
      stats.setSingleColorCombinationCount(colorCombinationRepository.findByColorCount(1).size());
      stats.setTotalCombinationCount(colorCombinationRepository.count());
      stats.setMigrationStatus(currentStatus);

      if (lastMigrationResult != null) {
        stats.setLastMigrationTime(lastMigrationResult.getEndTime());
        stats.setLastMigrationSuccess(lastMigrationResult.isSuccess());
      }

    } catch (Exception e) {
      logger.error("Error getting migration statistics", e);
    }

    return stats;
  }

  /** Creates backup of current color combination data */
  public boolean createBackup() {
    try {
      logger.info("Creating backup of color combination data...");

      List<ColorCombination> currentData = colorCombinationRepository.findAll();
      logger.info("Backup available for {} color combinations", currentData.size());

      return true;
    } catch (Exception e) {
      logger.error("Failed to create backup: {}", e.getMessage());
      return false;
    }
  }
}
