package dev.kreaker.kolors.migration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test to validate that migration cleanup was successful and legacy code has
 * been removed.
 */
@SpringBootTest
@ActiveProfiles("test")
class PostMigrationValidationTest {

    @Autowired
    private DatabaseMigrationService migrationService;

    @Test
    void shouldConfirmLegacyDataHasBeenMigrated() {
        // Given - Migration has been completed and legacy code removed

        // When - Check if legacy data is present
        boolean hasLegacyData = migrationService.isLegacyDataPresent();

        // Then - Should return false as legacy data has been migrated and removed
        assertThat(hasLegacyData).isFalse();
    }

    @Test
    void shouldProvideValidMigrationStatistics() {
        // Given - Migration has been completed

        // When - Get migration statistics
        MigrationStatistics stats = migrationService.getMigrationStatistics();

        // Then - Should show no legacy records and current migration status
        assertThat(stats).isNotNull();
        assertThat(stats.getLegacyRecordCount()).isEqualTo(0);
        assertThat(stats.getMigrationStatus()).isEqualTo(MigrationStatus.COMPLETED);
    }

    @Test
    void shouldValidateDataIntegrity() {
        // Given - Migration has been completed

        // When - Validate migrated data
        MigrationResult result = migrationService.validateMigratedData();

        // Then - Validation should complete successfully
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldCreateBackupSuccessfully() {
        // Given - Current data exists

        // When - Create backup
        boolean backupCreated = migrationService.createBackup();

        // Then - Backup should be created successfully
        assertThat(backupCreated).isTrue();
    }
}
