package dev.kreaker.kolors.migration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.kreaker.kolors.ColorCombination;
import dev.kreaker.kolors.ColorCombinationRepository;
import dev.kreaker.kolors.ColorInCombination;
import dev.kreaker.kolors.KolorKombination;
import dev.kreaker.kolors.KolorKombinationRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MigrationIntegrationTest {

    @Autowired
    private DatabaseMigrationService migrationService;

    @Autowired
    private KolorKombinationRepository legacyRepository;

    @Autowired
    private ColorCombinationRepository colorCombinationRepository;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        colorCombinationRepository.deleteAll();
        legacyRepository.deleteAll();
        migrationService.resetMigrationStatus();
    }

    @Test
    void shouldMigrateCompleteDataSetSuccessfully() {
        // Given - Create legacy data
        KolorKombination legacy1 = new KolorKombination("Red Color", "FF0000");
        KolorKombination legacy2 = new KolorKombination("Green Color", "00FF00");
        KolorKombination legacy3 = new KolorKombination("Blue Color", "0000FF");

        legacyRepository.save(legacy1);
        legacyRepository.save(legacy2);
        legacyRepository.save(legacy3);

        // Verify initial state
        assertThat(legacyRepository.count()).isEqualTo(3);
        assertThat(colorCombinationRepository.count()).isEqualTo(0);
        assertThat(migrationService.isLegacyDataPresent()).isTrue();
        assertThat(migrationService.getMigrationStatus()).isEqualTo(MigrationStatus.NOT_STARTED);

        // When - Perform migration
        MigrationResult result = migrationService.migrateLegacyData();

        // Then - Verify migration results
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalLegacyRecords()).isEqualTo(3);
        assertThat(result.getMigratedRecords()).isEqualTo(3);
        assertThat(result.getFailedRecords()).isEqualTo(0);
        assertThat(result.getErrors()).isEmpty();
        assertThat(migrationService.getMigrationStatus()).isEqualTo(MigrationStatus.COMPLETED);

        // Verify migrated data structure
        List<ColorCombination> migratedCombinations = colorCombinationRepository.findAll();
        assertThat(migratedCombinations).hasSize(3);

        // Verify each migrated combination
        for (ColorCombination combination : migratedCombinations) {
            assertThat(combination.getName()).isNotNull();
            assertThat(combination.getColorCount()).isEqualTo(1);
            assertThat(combination.getColors()).hasSize(1);
            assertThat(combination.getCreatedAt()).isNotNull();

            ColorInCombination color = combination.getColors().get(0);
            assertThat(color.getHexValue()).matches("^[0-9A-F]{6}$");
            assertThat(color.getPosition()).isEqualTo(1);
            assertThat(color.getCombination()).isEqualTo(combination);
        }

        // Verify specific color values
        List<String> migratedHexValues = migratedCombinations.stream()
                .flatMap(c -> c.getColors().stream())
                .map(ColorInCombination::getHexValue)
                .toList();

        assertThat(migratedHexValues).containsExactlyInAnyOrder("FF0000", "00FF00", "0000FF");
    }

    @Test
    void shouldHandleDuplicateMigrationGracefully() {
        // Given - Create legacy data
        KolorKombination legacy = new KolorKombination("Test Color", "ABCDEF");
        legacyRepository.save(legacy);

        // Perform first migration
        MigrationResult firstResult = migrationService.migrateLegacyData();
        assertThat(firstResult.isSuccess()).isTrue();
        assertThat(firstResult.getMigratedRecords()).isEqualTo(1);

        // Reset status to simulate another migration attempt
        migrationService.resetMigrationStatus();

        // When - Perform second migration
        MigrationResult secondResult = migrationService.migrateLegacyData();

        // Then - Should detect already migrated data
        assertThat(secondResult.isSuccess()).isTrue();
        assertThat(secondResult.getMigratedRecords()).isEqualTo(1); // Still considered successful
        assertThat(secondResult.getWarnings()).hasSize(1);
        assertThat(secondResult.getWarnings().get(0)).contains("already migrated");

        // Should not create duplicate combinations
        assertThat(colorCombinationRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldHandleMixedValidAndInvalidData() {
        // Given - Create mixed legacy data
        KolorKombination validLegacy = new KolorKombination("Valid Color", "FF0000");
        KolorKombination invalidNameLegacy = new KolorKombination("AB", "00FF00"); // Name too short
        KolorKombination invalidHexLegacy = new KolorKombination("Invalid Hex", "GGGGGG"); // Invalid hex

        legacyRepository.save(validLegacy);
        legacyRepository.save(invalidNameLegacy);
        legacyRepository.save(invalidHexLegacy);

        // When - Perform migration
        MigrationResult result = migrationService.migrateLegacyData();

        // Then - Should migrate valid records and report errors for invalid ones
        assertThat(result.isSuccess()).isTrue(); // Success because some records were migrated
        assertThat(result.getTotalLegacyRecords()).isEqualTo(3);
        assertThat(result.getMigratedRecords()).isEqualTo(1);
        assertThat(result.getFailedRecords()).isEqualTo(2);
        assertThat(result.getErrors()).hasSize(2);
        assertThat(migrationService.getMigrationStatus()).isEqualTo(MigrationStatus.COMPLETED_WITH_ERRORS);

        // Verify only valid record was migrated
        List<ColorCombination> migratedCombinations = colorCombinationRepository.findAll();
        assertThat(migratedCombinations).hasSize(1);
        assertThat(migratedCombinations.get(0).getName()).isEqualTo("Valid Color");
        assertThat(migratedCombinations.get(0).getColors().get(0).getHexValue()).isEqualTo("FF0000");
    }

    @Test
    void shouldProvideAccurateMigrationStatistics() {
        // Given - Create legacy data and some existing combinations
        KolorKombination legacy1 = new KolorKombination("Legacy 1", "FF0000");
        KolorKombination legacy2 = new KolorKombination("Legacy 2", "00FF00");
        legacyRepository.save(legacy1);
        legacyRepository.save(legacy2);

        // Create some existing multi-color combinations
        ColorCombination existingCombo = new ColorCombination("Existing Combo");
        existingCombo.addColorAtPosition("ABCDEF", 1);
        existingCombo.addColorAtPosition("123456", 2);
        colorCombinationRepository.save(existingCombo);

        // When - Get statistics before migration
        MigrationStatistics statsBefore = migrationService.getMigrationStatistics();

        // Then - Verify pre-migration statistics
        assertThat(statsBefore.getLegacyRecordCount()).isEqualTo(2);
        assertThat(statsBefore.getSingleColorCombinationCount()).isEqualTo(0);
        assertThat(statsBefore.getTotalCombinationCount()).isEqualTo(1);
        assertThat(statsBefore.getMigrationStatus()).isEqualTo(MigrationStatus.NOT_STARTED);
        assertThat(statsBefore.isMigrationNeeded()).isTrue();

        // Perform migration
        MigrationResult result = migrationService.migrateLegacyData();
        assertThat(result.isSuccess()).isTrue();

        // Get statistics after migration
        MigrationStatistics statsAfter = migrationService.getMigrationStatistics();

        // Verify post-migration statistics
        assertThat(statsAfter.getLegacyRecordCount()).isEqualTo(2);
        assertThat(statsAfter.getSingleColorCombinationCount()).isEqualTo(2);
        assertThat(statsAfter.getTotalCombinationCount()).isEqualTo(3); // 2 migrated + 1 existing
        assertThat(statsAfter.getMigrationStatus()).isEqualTo(MigrationStatus.COMPLETED);
        assertThat(statsAfter.isMigrationComplete()).isTrue();
        assertThat(statsAfter.getMigrationProgress()).isEqualTo(100.0);
        assertThat(statsAfter.getLastMigrationTime()).isNotNull();
        assertThat(statsAfter.isLastMigrationSuccess()).isTrue();
    }

    @Test
    void shouldHandleEmptyLegacyData() {
        // Given - No legacy data
        assertThat(legacyRepository.count()).isEqualTo(0);

        // When - Perform migration
        MigrationResult result = migrationService.migrateLegacyData();

        // Then - Should handle gracefully
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalLegacyRecords()).isEqualTo(0);
        assertThat(result.getMigratedRecords()).isEqualTo(0);
        assertThat(result.getFailedRecords()).isEqualTo(0);
        assertThat(migrationService.getMigrationStatus()).isEqualTo(MigrationStatus.NO_LEGACY_DATA);
        assertThat(migrationService.isLegacyDataPresent()).isFalse();

        // Should not create any combinations
        assertThat(colorCombinationRepository.count()).isEqualTo(0);
    }

    @Test
    void shouldPerformPostMigrationValidation() {
        // Given - Create legacy data with edge cases
        KolorKombination legacy1 = new KolorKombination("Test Color 1", "abcdef"); // Lowercase hex
        KolorKombination legacy2 = new KolorKombination("Test Color 2", "123456");
        legacyRepository.save(legacy1);
        legacyRepository.save(legacy2);

        // When - Perform migration
        MigrationResult result = migrationService.migrateLegacyData();

        // Then - Should pass validation
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getErrors()).isEmpty();

        // Verify hex values are normalized to uppercase
        List<ColorCombination> combinations = colorCombinationRepository.findAll();
        assertThat(combinations).hasSize(2);

        List<String> hexValues = combinations.stream()
                .flatMap(c -> c.getColors().stream())
                .map(ColorInCombination::getHexValue)
                .toList();

        assertThat(hexValues).containsExactlyInAnyOrder("ABCDEF", "123456");
    }

    @Test
    void shouldCreateBackupBeforeMigration() {
        // Given - Create legacy data
        KolorKombination legacy = new KolorKombination("Test Color", "FF0000");
        legacyRepository.save(legacy);

        // When - Create backup
        boolean backupCreated = migrationService.createBackup();

        // Then - Should succeed
        assertThat(backupCreated).isTrue();
    }
}
