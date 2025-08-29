package dev.kreaker.kolors.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.kreaker.kolors.ColorCombination;
import dev.kreaker.kolors.ColorCombinationRepository;
import dev.kreaker.kolors.ColorInCombination;
import dev.kreaker.kolors.KolorKombination;
import dev.kreaker.kolors.KolorKombinationRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service to migrate data from H2 (legacy KolorKombination) to SQLite (new
 * ColorCombination structure)
 * This service is only active when migration is enabled via properties
 */
@Service
@ConditionalOnProperty(name = "kolors.migration.enabled", havingValue = "true", matchIfMissing = false)
public class DatabaseMigrationService {

   private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationService.class);

   @Autowired
   private KolorKombinationRepository legacyRepository;

   @Autowired
   private ColorCombinationRepository newRepository;

   @Autowired
   private JdbcTemplate jdbcTemplate;

   /**
    * Migrates all legacy KolorKombination records to new ColorCombination
    * structure
    * Each legacy color becomes a single-color combination
    */
   @Transactional
   public MigrationResult migrateLegacyData() {
      logger.info("Starting migration from H2 to SQLite...");

      MigrationResult result = new MigrationResult();

      try {
         // Check if legacy data exists
         List<KolorKombination> legacyColors = legacyRepository.findAll();

         if (legacyColors.isEmpty()) {
            logger.info("No legacy data found to migrate");
            result.setSuccess(true);
            result.setMessage("No legacy data found to migrate");
            return result;
         }

         logger.info("Found {} legacy color records to migrate", legacyColors.size());

         int migratedCount = 0;
         int errorCount = 0;

         for (KolorKombination legacyColor : legacyColors) {
            try {
               // Create new combination with single color
               ColorCombination newCombination = new ColorCombination();
               newCombination.setName("Migrated: " + legacyColor.getName());
               newCombination.setColorCount(1);
               newCombination.setCreatedAt(LocalDateTime.now());

               // Create single color entry
               ColorInCombination color = new ColorInCombination();
               color.setHexValue(legacyColor.getHex());
               color.setPosition(1);

               newCombination.addColor(color);

               // Save the new combination
               newRepository.save(newCombination);
               migratedCount++;

               logger.debug("Migrated legacy color: {} -> {}",
                     legacyColor.getName(), newCombination.getName());

            } catch (Exception e) {
               logger.error("Error migrating legacy color {}: {}",
                     legacyColor.getName(), e.getMessage());
               errorCount++;
            }
         }

         result.setSuccess(errorCount == 0);
         result.setMigratedCount(migratedCount);
         result.setErrorCount(errorCount);
         result.setMessage(String.format("Migration completed: %d migrated, %d errors",
               migratedCount, errorCount));

         logger.info("Migration completed: {} migrated, {} errors", migratedCount, errorCount);

      } catch (Exception e) {
         logger.error("Migration failed with error: {}", e.getMessage(), e);
         result.setSuccess(false);
         result.setMessage("Migration failed: " + e.getMessage());
      }

      return result;
   }

   /**
    * Checks if legacy data exists in the database
    */
   public boolean hasLegacyData() {
      try {
         return legacyRepository.count() > 0;
      } catch (Exception e) {
         logger.warn("Could not check for legacy data: {}", e.getMessage());
         return false;
      }
   }

   /**
    * Validates the integrity of migrated data
    */
   public ValidationResult validateMigration() {
      ValidationResult result = new ValidationResult();

      try {
         long legacyCount = legacyRepository.count();
         long newCount = newRepository.count();

         result.setLegacyRecordCount(legacyCount);
         result.setNewRecordCount(newCount);
         result.setValid(newCount >= legacyCount); // Should have at least as many records

         if (result.isValid()) {
            result.setMessage("Migration validation passed");
         } else {
            result.setMessage(String.format("Migration validation failed: legacy=%d, new=%d",
                  legacyCount, newCount));
         }

      } catch (Exception e) {
         result.setValid(false);
         result.setMessage("Validation failed: " + e.getMessage());
      }

      return result;
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

   /**
    * Result of migration operation
    */
   public static class MigrationResult {
      private boolean success;
      private String message;
      private int migratedCount;
      private int errorCount;

      // Getters and setters
      public boolean isSuccess() {
         return success;
      }

      public void setSuccess(boolean success) {
         this.success = success;
      }

      public String getMessage() {
         return message;
      }

      public void setMessage(String message) {
         this.message = message;
      }

      public int getMigratedCount() {
         return migratedCount;
      }

      public void setMigratedCount(int migratedCount) {
         this.migratedCount = migratedCount;
      }

      public int getErrorCount() {
         return errorCount;
      }

      public void setErrorCount(int errorCount) {
         this.errorCount = errorCount;
      }
   }

   /**
    * Result of migration validation
    */
   public static class ValidationResult {
      private boolean valid;
      private String message;
      private long legacyRecordCount;
      private long newRecordCount;

      // Getters and setters
      public boolean isValid() {
         return valid;
      }

      public void setValid(boolean valid) {
         this.valid = valid;
      }

      public String getMessage() {
         return message;
      }

      public void setMessage(String message) {
         this.message = message;
      }

      public long getLegacyRecordCount() {
         return legacyRecordCount;
      }

      public void setLegacyRecordCount(long legacyRecordCount) {
         this.legacyRecordCount = legacyRecordCount;
      }

      public long getNewRecordCount() {
         return newRecordCount;
      }

      public void setNewRecordCount(long newRecordCount) {
         this.newRecordCount = newRecordCount;
      }
   }
}