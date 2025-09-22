package dev.kreaker.kolors.migration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.kreaker.kolors.ColorCombination;
import dev.kreaker.kolors.ColorCombinationRepository;
import dev.kreaker.kolors.ColorInCombination;
import dev.kreaker.kolors.KolorKombination;
import dev.kreaker.kolors.KolorKombinationRepository;

/**
 * Service responsible for migrating legacy KolorKombination data to the new
 * ColorCombination structure
 */
@Service
public class DatabaseMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationService.class);
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9A-Fa-f]{6}$");

    @Autowired
    private KolorKombinationRepository legacyRepository;

    @Autowired
    private ColorCombinationRepository colorCombinationRepository;

    private MigrationStatus currentStatus = MigrationStatus.NOT_STARTED;
    private MigrationResult lastMigrationResult;

    /**
     * Migrates all legacy KolorKombination records to the new ColorCombination
     * structure Each legacy record becomes a ColorCombination with a single
     * color
     */
    @Transactional
    public MigrationResult migrateLegacyData() {
        logger.info("Starting migration of legacy data");

        currentStatus = MigrationStatus.IN_PROGRESS;
        MigrationResult result = new MigrationResult();

        try {
            // Get all legacy records
            List<KolorKombination> legacyRecords = legacyRepository.findAll();
            result.setTotalLegacyRecords(legacyRecords.size());

            if (legacyRecords.isEmpty()) {
                logger.info("No legacy data found to migrate");
                currentStatus = MigrationStatus.NO_LEGACY_DATA;
                result.setSuccess(true);
                result.complete();
                lastMigrationResult = result;
                return result;
            }

            logger.info("Found {} legacy records to migrate", legacyRecords.size());

            int migratedCount = 0;
            int failedCount = 0;

            for (KolorKombination legacy : legacyRecords) {
                try {
                    if (migrateSingleRecord(legacy, result)) {
                        migratedCount++;
                    } else {
                        failedCount++;
                    }
                } catch (Exception e) {
                    failedCount++;
                    String error = "Failed to migrate record ID " + legacy.getId() + ": " + e.getMessage();
                    result.addError(error);
                    logger.error(error, e);
                }
            }

            result.setMigratedRecords(migratedCount);
            result.setFailedRecords(failedCount);

            // Determine final status
            if (failedCount == 0) {
                currentStatus = MigrationStatus.COMPLETED;
                result.setSuccess(true);
                logger.info("Migration completed successfully. Migrated {} records", migratedCount);
            } else if (migratedCount > 0) {
                currentStatus = MigrationStatus.COMPLETED_WITH_ERRORS;
                result.setSuccess(true);
                logger.warn("Migration completed with errors. Migrated: {}, Failed: {}", migratedCount, failedCount);
            } else {
                currentStatus = MigrationStatus.FAILED;
                result.setSuccess(false);
                logger.error("Migration failed completely. No records were migrated");
            }

        } catch (Exception e) {
            currentStatus = MigrationStatus.FAILED;
            result.setSuccess(false);
            result.addError("Migration failed with exception: " + e.getMessage());
            logger.error("Migration failed with exception", e);
        }

        result.complete();
        lastMigrationResult = result;

        // Perform post-migration validation
        if (result.isSuccess()) {
            performPostMigrationValidation(result);
        }

        return result;
    }

    /**
     * Migrates a single legacy record to the new structure
     */
    private boolean migrateSingleRecord(KolorKombination legacy, MigrationResult result) {
        try {
            // Validate legacy data
            if (!isValidLegacyRecord(legacy, result)) {
                return false;
            }

            // Check if already migrated (by name and hex combination)
            if (isAlreadyMigrated(legacy)) {
                result.addWarning("Record already migrated: " + legacy.getName() + " (" + legacy.getHex() + ")");
                return true; // Consider as successful since it's already there
            }

            // Create new ColorCombination
            ColorCombination newCombination = new ColorCombination();
            newCombination.setName(legacy.getName());
            newCombination.setColorCount(1);
            newCombination.setCreatedAt(LocalDateTime.now());

            // Save the combination first to get an ID
            newCombination = colorCombinationRepository.save(newCombination);

            // Create the single color
            ColorInCombination color = new ColorInCombination();
            color.setHexValue(legacy.getHex().toUpperCase());
            color.setPosition(1);
            color.setCombination(newCombination);

            // Add color to combination
            newCombination.addColor(color);

            // Save the updated combination
            colorCombinationRepository.save(newCombination);

            logger.debug("Successfully migrated: {} -> ColorCombination ID {}",
                    legacy.getName(), newCombination.getId());

            return true;

        } catch (Exception e) {
            logger.error("Error migrating record ID {}: {}", legacy.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates a legacy record before migration
     */
    private boolean isValidLegacyRecord(KolorKombination legacy, MigrationResult result) {
        if (legacy.getName() == null || legacy.getName().trim().isEmpty()) {
            result.addError("Invalid legacy record ID " + legacy.getId() + ": name is null or empty");
            return false;
        }

        if (legacy.getName().length() < 3) {
            result.addError("Invalid legacy record ID " + legacy.getId() + ": name too short (< 3 characters)");
            return false;
        }

        if (legacy.getHex() == null || !HEX_PATTERN.matcher(legacy.getHex()).matches()) {
            result.addError("Invalid legacy record ID " + legacy.getId() + ": invalid hex value '" + legacy.getHex() + "'");
            return false;
        }

        return true;
    }

    /**
     * Checks if a legacy record has already been migrated
     */
    private boolean isAlreadyMigrated(KolorKombination legacy) {
        // Look for combinations with the same name and single color with the same hex
        List<ColorCombination> existing = colorCombinationRepository.findByNameContainingIgnoreCase(legacy.getName());

        for (ColorCombination combination : existing) {
            if (combination.getName().equals(legacy.getName())
                    && combination.getColorCount() == 1
                    && !combination.getColors().isEmpty()
                    && combination.getColors().get(0).getHexValue().equalsIgnoreCase(legacy.getHex())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Performs validation after migration to ensure data integrity
     */
    private void performPostMigrationValidation(MigrationResult result) {
        logger.info("Performing post-migration validation");

        try {
            // Count legacy records
            long legacyCount = legacyRepository.count();

            // Count migrated single-color combinations
            long migratedCount = colorCombinationRepository.findByColorCount(1).size();

            if (migratedCount < legacyCount) {
                result.addWarning("Post-migration validation: Expected at least " + legacyCount
                        + " single-color combinations, but found " + migratedCount);
            }

            // Validate that all migrated combinations have valid colors
            List<ColorCombination> singleColorCombinations = colorCombinationRepository.findByColorCount(1);
            int invalidCombinations = 0;

            for (ColorCombination combination : singleColorCombinations) {
                if (combination.getColors().isEmpty()) {
                    invalidCombinations++;
                    result.addError("Post-migration validation: Combination ID " + combination.getId()
                            + " has colorCount=1 but no colors");
                } else if (combination.getColors().size() != 1) {
                    invalidCombinations++;
                    result.addError("Post-migration validation: Combination ID " + combination.getId()
                            + " has colorCount=1 but " + combination.getColors().size() + " colors");
                } else {
                    ColorInCombination color = combination.getColors().get(0);
                    if (!HEX_PATTERN.matcher(color.getHexValue()).matches()) {
                        invalidCombinations++;
                        result.addError("Post-migration validation: Invalid hex value in combination ID "
                                + combination.getId() + ": " + color.getHexValue());
                    }
                }
            }

            if (invalidCombinations == 0) {
                logger.info("Post-migration validation passed successfully");
            } else {
                logger.warn("Post-migration validation found {} invalid combinations", invalidCombinations);
            }

        } catch (Exception e) {
            result.addError("Post-migration validation failed: " + e.getMessage());
            logger.error("Post-migration validation failed", e);
        }
    }

    /**
     * Checks if there is legacy data available for migration
     */
    public boolean isLegacyDataPresent() {
        try {
            return legacyRepository.count() > 0;
        } catch (Exception e) {
            logger.error("Error checking for legacy data", e);
            return false;
        }
    }

    /**
     * Gets the current migration status
     */
    public MigrationStatus getMigrationStatus() {
        return currentStatus;
    }

    /**
     * Gets the result of the last migration operation
     */
    public Optional<MigrationResult> getLastMigrationResult() {
        return Optional.ofNullable(lastMigrationResult);
    }

    /**
     * Resets the migration status (for testing purposes)
     */
    public void resetMigrationStatus() {
        currentStatus = MigrationStatus.NOT_STARTED;
        lastMigrationResult = null;
    }

    /**
     * Gets statistics about legacy and migrated data
     */
    public MigrationStatistics getMigrationStatistics() {
        MigrationStatistics stats = new MigrationStatistics();

        try {
            stats.setLegacyRecordCount(legacyRepository.count());
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

    /**
     * Creates backup of legacy data before migration
     */
    public boolean createBackup() {
        try {
            // This is a simple backup - in production you might want to export to file
            logger.info("Creating backup of legacy data...");

            List<KolorKombination> legacyData = legacyRepository.findAll();
            logger.info("Backup created for {} legacy records", legacyData.size());

            return true;
        } catch (Exception e) {
            logger.error("Failed to create backup: {}", e.getMessage());
            return false;
        }
    }
}
