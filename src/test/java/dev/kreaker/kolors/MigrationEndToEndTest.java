package dev.kreaker.kolors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import dev.kreaker.kolors.migration.DatabaseMigrationService;
import dev.kreaker.kolors.migration.MigrationResult;
import dev.kreaker.kolors.migration.MigrationStatistics;

/**
 * Comprehensive End-to-End Migration Tests Tests migration scenarios with
 * different data configurations
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("Migration End-to-End Tests")
class MigrationEndToEndTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DatabaseMigrationService migrationService;

    @Autowired
    private KolorKombinationRepository legacyRepository;

    @Autowired
    private ColorCombinationRepository newRepository;

    @Autowired
    private ColorInCombinationRepository colorRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Clean all data before each test
        colorRepository.deleteAll();
        newRepository.deleteAll();
        legacyRepository.deleteAll();
    }

    @Nested
    @DisplayName("Basic Migration Scenarios")
    class BasicMigrationScenariosTests {

        @Test
        @DisplayName("Should migrate empty legacy database successfully")
        void shouldMigrateEmptyLegacyDatabaseSuccessfully() {
            // Given - empty legacy database
            assertThat(legacyRepository.count()).isZero();

            // When
            MigrationResult result = migrationService.migrateLegacyData();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMigratedRecords()).isZero();
            assertThat(result.getFailedRecords()).isZero();
            assertThat(result.getErrors()).isEmpty();

            // Verify new database is still empty
            assertThat(newRepository.count()).isZero();
            assertThat(colorRepository.count()).isZero();
        }

        @Test
        @DisplayName("Should migrate single legacy color successfully")
        void shouldMigrateSingleLegacyColorSuccessfully() {
            // Given - single legacy color
            KolorKombination legacy = new KolorKombination();
            legacy.setName("Legacy Red");
            legacy.setHex("FF0000");
            legacyRepository.save(legacy);

            // When
            MigrationResult result = migrationService.migrateLegacyData();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMigratedRecords()).isEqualTo(1);
            assertThat(result.getFailedRecords()).isZero();
            assertThat(result.getErrors()).isEmpty();

            // Verify migration
            List<ColorCombination> newCombinations = newRepository.findAll();
            assertThat(newCombinations).hasSize(1);

            ColorCombination migrated = newCombinations.get(0);
            assertThat(migrated.getName()).isEqualTo("Legacy Red");
            assertThat(migrated.getColorCount()).isEqualTo(1);
            assertThat(migrated.getColors()).hasSize(1);
            assertThat(migrated.getColors().get(0).getHexValue()).isEqualTo("FF0000");
            assertThat(migrated.getColors().get(0).getPosition()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should migrate multiple legacy colors successfully")
        void shouldMigrateMultipleLegacyColorsSuccessfully() {
            // Given - multiple legacy colors
            createLegacyColor("Red Color", "FF0000");
            createLegacyColor("Green Color", "00FF00");
            createLegacyColor("Blue Color", "0000FF");
            createLegacyColor("Yellow Color", "FFFF00");

            // When
            MigrationResult result = migrationService.migrateLegacyData();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMigratedRecords()).isEqualTo(4);
            assertThat(result.getFailedRecords()).isZero();
            assertThat(result.getErrors()).isEmpty();

            // Verify migration
            List<ColorCombination> newCombinations = newRepository.findAll();
            assertThat(newCombinations).hasSize(4);

            // Verify each combination has exactly one color
            assertThat(newCombinations).allMatch(c -> c.getColorCount() == 1);
            assertThat(newCombinations).allMatch(c -> c.getColors().size() == 1);

            // Verify total colors
            assertThat(colorRepository.count()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Edge Case Migration Scenarios")
    class EdgeCaseMigrationScenariosTests {

        @Test
        @DisplayName("Should handle legacy colors with invalid hex values")
        void shouldHandleLegacyColorsWithInvalidHexValues() {
            // Given - legacy colors with various invalid hex values
            createLegacyColor("Invalid Short", "FF0");
            createLegacyColor("Invalid Long", "FF00000");
            createLegacyColor("Invalid Chars", "GGHHII");
            createLegacyColor("Valid Color", "FF0000");

            // When
            MigrationResult result = migrationService.migrateLegacyData();

            // Then
            assertThat(result.isSuccess()).isFalse(); // Overall failure due to invalid data
            assertThat(result.getMigratedRecords()).isEqualTo(1); // Only valid one migrated
            assertThat(result.getFailedRecords()).isEqualTo(3);
            assertThat(result.getErrors()).hasSize(3);

            // Verify only valid color was migrated
            List<ColorCombination> newCombinations = newRepository.findAll();
            assertThat(newCombinations).hasSize(1);
            assertThat(newCombinations.get(0).getName()).isEqualTo("Valid Color");
        }

        @Test
        @DisplayName("Should handle legacy colors with null or empty names")
        void shouldHandleLegacyColorsWithNullOrEmptyNames() {
            // Given - legacy colors with problematic names
            KolorKombination nullName = new KolorKombination();
            nullName.setName(null);
            nullName.setHex("FF0000");
            legacyRepository.save(nullName);

            KolorKombination emptyName = new KolorKombination();
            emptyName.setName("");
            emptyName.setHex("00FF00");
            legacyRepository.save(emptyName);

            KolorKombination shortName = new KolorKombination();
            shortName.setName("AB");
            shortName.setHex("0000FF");
            legacyRepository.save(shortName);

            createLegacyColor("Valid Name", "FFFF00");

            // When
            MigrationResult result = migrationService.migrateLegacyData();

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMigratedRecords()).isEqualTo(1); // Only valid one
            assertThat(result.getFailedRecords()).isEqualTo(3);
            assertThat(result.getErrors()).hasSize(3);

            // Verify only valid color was migrated
            List<ColorCombination> newCombinations = newRepository.findAll();
            assertThat(newCombinations).hasSize(1);
            assertThat(newCombinations.get(0).getName()).isEqualTo("Valid Name");
        }

        @Test
        @DisplayName("Should handle legacy colors with duplicate names")
        void shouldHandleLegacyColorsWithDuplicateNames() {
            // Given - legacy colors with duplicate names
            createLegacyColor("Duplicate Name", "FF0000");
            createLegacyColor("Duplicate Name", "00FF00");
            createLegacyColor("Duplicate Name", "0000FF");
            createLegacyColor("Unique Name", "FFFF00");

            // When
            MigrationResult result = migrationService.migrateLegacyData();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMigratedRecords()).isEqualTo(4);
            assertThat(result.getFailedRecords()).isZero();

            // Verify all colors were migrated (duplicates allowed in new system)
            List<ColorCombination> newCombinations = newRepository.findAll();
            assertThat(newCombinations).hasSize(4);

            // Verify duplicate names exist
            long duplicateCount = newCombinations.stream()
                    .filter(c -> "Duplicate Name".equals(c.getName()))
                    .count();
            assertThat(duplicateCount).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle legacy colors with null creation dates")
        void shouldHandleLegacyColorsWithNullCreationDates() {
            // Given - legacy color
            KolorKombination nullDate = new KolorKombination();
            nullDate.setName("Null Date Color");
            nullDate.setHex("FF0000");
            legacyRepository.save(nullDate);

            // When
            MigrationResult result = migrationService.migrateLegacyData();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMigratedRecords()).isEqualTo(1);

            // Verify migration
            ColorCombination migrated = newRepository.findAll().get(0);
            assertThat(migrated.getCreatedAt()).isNotNull();
            assertThat(migrated.getName()).isEqualTo("Null Date Color");
        }
    }

    @Nested
    @DisplayName("Large Dataset Migration Scenarios")
    class LargeDatasetMigrationScenariosTests {

        @Test
        @DisplayName("Should migrate large dataset efficiently")
        void shouldMigrateLargeDatasetEfficiently() {
            // Given - large dataset of legacy colors
            int largeDatasetSize = 1000;
            for (int i = 0; i < largeDatasetSize; i++) {
                createLegacyColor("Color " + i, String.format("%06X", i * 1000));
            }

            // When
            long startTime = System.currentTimeMillis();
            MigrationResult result = migrationService.migrateLegacyData();
            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMigratedRecords()).isEqualTo(largeDatasetSize);
            assertThat(result.getFailedRecords()).isZero();

            // Verify performance (should complete within 10 seconds)
            assertThat(duration).isLessThan(10000);

            // Verify data integrity
            assertThat(newRepository.count()).isEqualTo(largeDatasetSize);
            assertThat(colorRepository.count()).isEqualTo(largeDatasetSize);
        }

        @Test
        @DisplayName("Should handle mixed valid and invalid data in large dataset")
        void shouldHandleMixedValidAndInvalidDataInLargeDataset() {
            // Given - mixed dataset with some invalid entries
            int totalSize = 500;
            int invalidCount = 0;

            for (int i = 0; i < totalSize; i++) {
                if (i % 10 == 0) {
                    // Create invalid entry (every 10th)
                    createLegacyColor("Invalid " + i, "INVALID");
                    invalidCount++;
                } else {
                    // Create valid entry
                    createLegacyColor("Valid " + i, String.format("%06X", i * 1000));
                }
            }

            // When
            MigrationResult result = migrationService.migrateLegacyData();

            // Then
            assertThat(result.isSuccess()).isFalse(); // Overall failure due to invalid data
            assertThat(result.getMigratedRecords()).isEqualTo(totalSize - invalidCount);
            assertThat(result.getFailedRecords()).isEqualTo(invalidCount);
            assertThat(result.getErrors()).hasSize(invalidCount);

            // Verify only valid data was migrated
            assertThat(newRepository.count()).isEqualTo(totalSize - invalidCount);
        }
    }

    @Nested
    @DisplayName("Migration Controller End-to-End")
    class MigrationControllerE2ETests {

        @Test
        @DisplayName("Should display migration status page")
        void shouldDisplayMigrationStatusPage() throws Exception {
            // Given - some legacy data
            createLegacyColor("Test Color", "FF0000");

            // When & Then
            mockMvc.perform(get("/admin/migration/status"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/migration-status"))
                    .andExpect(model().attributeExists("migrationStatus"))
                    .andExpect(model().attributeExists("statistics"));
        }

        @Test
        @DisplayName("Should execute migration via controller")
        void shouldExecuteMigrationViaController() throws Exception {
            // Given - legacy data
            createLegacyColor("Controller Test", "FF0000");
            createLegacyColor("Another Color", "00FF00");

            // When
            mockMvc.perform(post("/admin/migration/execute"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/migration/status"))
                    .andExpect(flash().attributeExists("migrationResult"));

            // Then - verify migration occurred
            assertThat(newRepository.count()).isEqualTo(2);
            assertThat(colorRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle migration errors via controller")
        void shouldHandleMigrationErrorsViaController() throws Exception {
            // Given - invalid legacy data
            createLegacyColor("Valid Color", "FF0000");
            createLegacyColor("Invalid Color", "INVALID");

            // When
            mockMvc.perform(post("/admin/migration/execute"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/migration/status"))
                    .andExpect(flash().attributeExists("migrationResult"));

            // Then - verify partial migration
            assertThat(newRepository.count()).isEqualTo(1);

            // Verify error information is available
            Optional<MigrationResult> resultOpt = migrationService.getLastMigrationResult();
            assertThat(resultOpt).isPresent();
            MigrationResult result = resultOpt.get();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailedRecords()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should prevent duplicate migration execution")
        void shouldPreventDuplicateMigrationExecution() throws Exception {
            // Given - legacy data
            createLegacyColor("Test Color", "FF0000");

            // When - first migration
            mockMvc.perform(post("/admin/migration/execute"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("migrationResult"));

            // Verify first migration
            assertThat(newRepository.count()).isEqualTo(1);

            // When - second migration attempt
            mockMvc.perform(post("/admin/migration/execute"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/migration/status"));

            // Then - should not duplicate data
            assertThat(newRepository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Migration Statistics and Reporting")
    class MigrationStatisticsTests {

        @Test
        @DisplayName("Should generate accurate migration statistics")
        void shouldGenerateAccurateMigrationStatistics() {
            // Given - diverse legacy data
            createLegacyColor("Valid 1", "FF0000");
            createLegacyColor("Valid 2", "00FF00");
            createLegacyColor("Valid 3", "0000FF");

            // Create invalid data
            KolorKombination invalid = new KolorKombination();
            invalid.setName("Invalid");
            invalid.setHex("INVALID");
            legacyRepository.save(invalid);

            // When
            MigrationResult result = migrationService.migrateLegacyData();
            MigrationStatistics stats = migrationService.getMigrationStatistics();

            // Then
            assertThat(stats.getLegacyRecordCount()).isEqualTo(4);
            assertThat(stats.getSingleColorCombinationCount()).isEqualTo(3);
            assertThat(stats.getTotalCombinationCount()).isEqualTo(3);

            assertThat(result.getMigratedRecords()).isEqualTo(3);
            assertThat(result.getFailedRecords()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should track migration progress over time")
        void shouldTrackMigrationProgressOverTime() {
            // Given - initial data
            createLegacyColor("Initial Color", "FF0000");

            // When - first migration
            MigrationResult firstResult = migrationService.migrateLegacyData();
            MigrationStatistics firstStats = migrationService.getMigrationStatistics();

            // Then - verify first migration
            assertThat(firstResult.getMigratedRecords()).isEqualTo(1);
            assertThat(firstStats.getSingleColorCombinationCount()).isEqualTo(1);

            // Given - add more legacy data
            createLegacyColor("Additional Color", "00FF00");

            // When - second migration (should only migrate new data)
            MigrationResult secondResult = migrationService.migrateLegacyData();
            MigrationStatistics secondStats = migrationService.getMigrationStatistics();

            // Then - verify incremental migration
            assertThat(secondStats.getLegacyRecordCount()).isEqualTo(2);
            assertThat(secondStats.getSingleColorCombinationCount()).isEqualTo(2);
            assertThat(newRepository.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Post-Migration Validation")
    class PostMigrationValidationTests {

        @Test
        @DisplayName("Should validate data integrity after migration")
        void shouldValidateDataIntegrityAfterMigration() {
            // Given - legacy data with specific characteristics
            LocalDateTime specificDate = LocalDateTime.of(2023, 1, 15, 10, 30);

            KolorKombination legacy1 = new KolorKombination();
            legacy1.setName("Specific Color 1");
            legacy1.setHex("FF6B35");
            legacyRepository.save(legacy1);

            KolorKombination legacy2 = new KolorKombination();
            legacy2.setName("Specific Color 2");
            legacy2.setHex("87CEEB");
            legacyRepository.save(legacy2);

            // When
            MigrationResult result = migrationService.migrateLegacyData();

            // Then - validate complete data integrity
            assertThat(result.isSuccess()).isTrue();

            List<ColorCombination> migrated = newRepository.findAll();
            assertThat(migrated).hasSize(2);

            // Validate first combination
            ColorCombination first = migrated.stream()
                    .filter(c -> "Specific Color 1".equals(c.getName()))
                    .findFirst().orElseThrow();
            assertThat(first.getColorCount()).isEqualTo(1);
            assertThat(first.getColors()).hasSize(1);
            assertThat(first.getColors().get(0).getHexValue()).isEqualTo("FF6B35");
            assertThat(first.getColors().get(0).getPosition()).isEqualTo(1);
            assertThat(first.getCreatedAt()).isNotNull();

            // Validate second combination
            ColorCombination second = migrated.stream()
                    .filter(c -> "Specific Color 2".equals(c.getName()))
                    .findFirst().orElseThrow();
            assertThat(second.getColorCount()).isEqualTo(1);
            assertThat(second.getColors()).hasSize(1);
            assertThat(second.getColors().get(0).getHexValue()).isEqualTo("87CEEB");
            assertThat(second.getColors().get(0).getPosition()).isEqualTo(1);
            assertThat(second.getCreatedAt()).isNotNull();

            // Validate bidirectional relationships
            for (ColorCombination combination : migrated) {
                for (ColorInCombination color : combination.getColors()) {
                    assertThat(color.getCombination()).isEqualTo(combination);
                }
            }
        }

        @Test
        @DisplayName("Should ensure migrated data is searchable")
        void shouldEnsureMigratedDataIsSearchable() throws Exception {
            // Given - legacy data
            createLegacyColor("Searchable Red", "FF0000");
            createLegacyColor("Searchable Blue", "0000FF");
            createLegacyColor("Different Name", "00FF00");

            // When - migrate
            migrationService.migrateLegacyData();

            // Then - verify data is searchable via web interface
            mockMvc.perform(get("/combinations/")
                    .param("search", "Searchable"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("combinations"));

            mockMvc.perform(get("/combinations/")
                    .param("hexValue", "FF0000"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("combinations"));

            mockMvc.perform(get("/combinations/")
                    .param("colorCount", "1"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("combinations"));
        }
    }

    private void createLegacyColor(String name, String hexValue) {
        KolorKombination legacy = new KolorKombination();
        legacy.setName(name);
        legacy.setHex(hexValue);
        legacyRepository.save(legacy);
    }
}
