package dev.kreaker.kolors.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for managing data migration from legacy KolorKombination to new
 * ColorCombination structure
 */
@Controller
@RequestMapping("/admin/migration")
public class MigrationController {

    private static final Logger logger = LoggerFactory.getLogger(MigrationController.class);

    @Autowired
    private DatabaseMigrationService migrationService;

    /**
     * Shows the migration status page
     */
    @GetMapping("/status")
    public String migrationStatus(Model model) {
        logger.info("Accessing migration status page");

        try {
            MigrationStatistics stats = migrationService.getMigrationStatistics();
            model.addAttribute("statistics", stats);

            // Add migration status information
            model.addAttribute("migrationStatus", migrationService.getMigrationStatus());
            model.addAttribute("hasLegacyData", migrationService.isLegacyDataPresent());

            // Add last migration result if available
            migrationService.getLastMigrationResult().ifPresent(result -> {
                model.addAttribute("lastMigrationResult", result);
            });

            return "admin/migration-status";

        } catch (Exception e) {
            logger.error("Error loading migration status", e);
            model.addAttribute("error", "Error loading migration status: " + e.getMessage());
            return "admin/migration-status";
        }
    }

    /**
     * Performs the migration of legacy data
     */
    @PostMapping("/migrate")
    public String migrateLegacyData(RedirectAttributes redirectAttributes) {
        logger.info("Starting migration process via web interface");

        try {
            // Check if migration is already in progress
            if (migrationService.getMigrationStatus() == MigrationStatus.IN_PROGRESS) {
                redirectAttributes.addFlashAttribute("error", "Migration is already in progress");
                return "redirect:/admin/migration/status";
            }

            // Check if there's legacy data to migrate
            if (!migrationService.isLegacyDataPresent()) {
                redirectAttributes.addFlashAttribute("warning", "No legacy data found to migrate");
                return "redirect:/admin/migration/status";
            }

            // Create backup before migration
            boolean backupCreated = migrationService.createBackup();
            if (!backupCreated) {
                redirectAttributes.addFlashAttribute("warning", "Failed to create backup, but proceeding with migration");
            }

            // Perform migration
            MigrationResult result = migrationService.migrateLegacyData();

            if (result.isSuccess()) {
                if (result.getFailedRecords() > 0) {
                    redirectAttributes.addFlashAttribute("warning",
                            "Migration completed with some errors: " + result.getSummary());
                } else {
                    redirectAttributes.addFlashAttribute("success",
                            "Migration completed successfully: " + result.getSummary());
                }
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "Migration failed: " + result.getSummary());
            }

        } catch (Exception e) {
            logger.error("Migration failed with exception", e);
            redirectAttributes.addFlashAttribute("error", "Migration failed: " + e.getMessage());
        }

        return "redirect:/admin/migration/status";
    }

    /**
     * REST endpoint to get migration statistics as JSON
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public ResponseEntity<MigrationStatistics> getMigrationStatistics() {
        try {
            MigrationStatistics stats = migrationService.getMigrationStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting migration statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * REST endpoint to check if legacy data is present
     */
    @GetMapping("/api/legacy-data-check")
    @ResponseBody
    public ResponseEntity<LegacyDataCheckResponse> checkLegacyData() {
        try {
            boolean hasLegacyData = migrationService.isLegacyDataPresent();
            MigrationStatus status = migrationService.getMigrationStatus();

            LegacyDataCheckResponse response = new LegacyDataCheckResponse();
            response.setHasLegacyData(hasLegacyData);
            response.setMigrationStatus(status);
            response.setMigrationNeeded(hasLegacyData && status == MigrationStatus.NOT_STARTED);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking legacy data", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * REST endpoint to perform migration via AJAX
     */
    @PostMapping("/api/migrate")
    @ResponseBody
    public ResponseEntity<MigrationResult> performMigration() {
        logger.info("Starting migration via REST API");

        try {
            // Check if migration is already in progress
            if (migrationService.getMigrationStatus() == MigrationStatus.IN_PROGRESS) {
                MigrationResult errorResult = new MigrationResult();
                errorResult.setSuccess(false);
                errorResult.addError("Migration is already in progress");
                errorResult.complete();
                return ResponseEntity.badRequest().body(errorResult);
            }

            // Check if there's legacy data to migrate
            if (!migrationService.isLegacyDataPresent()) {
                MigrationResult noDataResult = new MigrationResult();
                noDataResult.setSuccess(true);
                noDataResult.addWarning("No legacy data found to migrate");
                noDataResult.complete();
                return ResponseEntity.ok(noDataResult);
            }

            // Perform migration
            MigrationResult result = migrationService.migrateLegacyData();
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Migration failed via REST API", e);
            MigrationResult errorResult = new MigrationResult();
            errorResult.setSuccess(false);
            errorResult.addError("Migration failed: " + e.getMessage());
            errorResult.complete();
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * REST endpoint to reset migration status (for testing)
     */
    @PostMapping("/api/reset")
    @ResponseBody
    public ResponseEntity<String> resetMigrationStatus() {
        try {
            migrationService.resetMigrationStatus();
            logger.info("Migration status reset successfully");
            return ResponseEntity.ok("Migration status reset successfully");
        } catch (Exception e) {
            logger.error("Error resetting migration status", e);
            return ResponseEntity.internalServerError().body("Error resetting migration status: " + e.getMessage());
        }
    }

    /**
     * Response class for legacy data check endpoint
     */
    public static class LegacyDataCheckResponse {

        private boolean hasLegacyData;
        private MigrationStatus migrationStatus;
        private boolean migrationNeeded;

        // Getters and Setters
        public boolean isHasLegacyData() {
            return hasLegacyData;
        }

        public void setHasLegacyData(boolean hasLegacyData) {
            this.hasLegacyData = hasLegacyData;
        }

        public MigrationStatus getMigrationStatus() {
            return migrationStatus;
        }

        public void setMigrationStatus(MigrationStatus migrationStatus) {
            this.migrationStatus = migrationStatus;
        }

        public boolean isMigrationNeeded() {
            return migrationNeeded;
        }

        public void setMigrationNeeded(boolean migrationNeeded) {
            this.migrationNeeded = migrationNeeded;
        }
    }
}
