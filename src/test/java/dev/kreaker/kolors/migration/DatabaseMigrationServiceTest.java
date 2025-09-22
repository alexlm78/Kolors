package dev.kreaker.kolors.migration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.kreaker.kolors.ColorCombination;
import dev.kreaker.kolors.ColorCombinationRepository;
import dev.kreaker.kolors.ColorInCombination;
import dev.kreaker.kolors.KolorKombination;
import dev.kreaker.kolors.KolorKombinationRepository;

@ExtendWith(MockitoExtension.class)
class DatabaseMigrationServiceTest {

    @Mock
    private KolorKombinationRepository legacyRepository;

    @Mock
    private ColorCombinationRepository colorCombinationRepository;

    @InjectMocks
    private DatabaseMigrationService migrationService;

    private KolorKombination validLegacyRecord;
    private KolorKombination invalidLegacyRecord;

    @BeforeEach
    void setUp() {
        validLegacyRecord = new KolorKombination("Valid Color", "FF0000");
        invalidLegacyRecord = new KolorKombination("", "INVALID");

        // Reset migration status before each test
        migrationService.resetMigrationStatus();
    }

    @Test
    void shouldReturnNoLegacyDataWhenNoRecordsExist() {
        // Given
        when(legacyRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        MigrationResult result = migrationService.migrateLegacyData();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalLegacyRecords()).isEqualTo(0);
        assertThat(result.getMigratedRecords()).isEqualTo(0);
        assertThat(result.getFailedRecords()).isEqualTo(0);
        assertThat(migrationService.getMigrationStatus()).isEqualTo(MigrationStatus.NO_LEGACY_DATA);

        verify(colorCombinationRepository, never()).save(any());
    }

    @Test
    void shouldMigrateValidLegacyRecordsSuccessfully() {
        // Given
        List<KolorKombination> legacyRecords = Arrays.asList(validLegacyRecord);
        when(legacyRepository.findAll()).thenReturn(legacyRecords);
        when(colorCombinationRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(Collections.emptyList());

        ColorCombination savedCombination = new ColorCombination("Valid Color");
        savedCombination.setId(1L);
        when(colorCombinationRepository.save(any(ColorCombination.class)))
                .thenReturn(savedCombination);

        when(colorCombinationRepository.findByColorCount(1))
                .thenReturn(Arrays.asList(savedCombination));
        when(legacyRepository.count()).thenReturn(1L);

        // When
        MigrationResult result = migrationService.migrateLegacyData();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalLegacyRecords()).isEqualTo(1);
        assertThat(result.getMigratedRecords()).isEqualTo(1);
        assertThat(result.getFailedRecords()).isEqualTo(0);
        assertThat(migrationService.getMigrationStatus()).isEqualTo(MigrationStatus.COMPLETED);

        verify(colorCombinationRepository, times(2)).save(any(ColorCombination.class));
    }

    @Test
    void shouldHandleInvalidLegacyRecords() {
        // Given
        List<KolorKombination> legacyRecords = Arrays.asList(invalidLegacyRecord);
        when(legacyRepository.findAll()).thenReturn(legacyRecords);

        // When
        MigrationResult result = migrationService.migrateLegacyData();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getTotalLegacyRecords()).isEqualTo(1);
        assertThat(result.getMigratedRecords()).isEqualTo(0);
        assertThat(result.getFailedRecords()).isEqualTo(1);
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(migrationService.getMigrationStatus()).isEqualTo(MigrationStatus.FAILED);

        verify(colorCombinationRepository, never()).save(any());
    }

    @Test
    void shouldHandleMixedValidAndInvalidRecords() {
        // Given
        List<KolorKombination> legacyRecords = Arrays.asList(validLegacyRecord, invalidLegacyRecord);
        when(legacyRepository.findAll()).thenReturn(legacyRecords);
        when(colorCombinationRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(Collections.emptyList());

        ColorCombination savedCombination = new ColorCombination("Valid Color");
        savedCombination.setId(1L);
        when(colorCombinationRepository.save(any(ColorCombination.class)))
                .thenReturn(savedCombination);

        when(colorCombinationRepository.findByColorCount(1))
                .thenReturn(Arrays.asList(savedCombination));
        when(legacyRepository.count()).thenReturn(2L);

        // When
        MigrationResult result = migrationService.migrateLegacyData();

        // Then
        assertThat(result.isSuccess()).isTrue(); // Success because some records were migrated
        assertThat(result.getTotalLegacyRecords()).isEqualTo(2);
        assertThat(result.getMigratedRecords()).isEqualTo(1);
        assertThat(result.getFailedRecords()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(migrationService.getMigrationStatus()).isEqualTo(MigrationStatus.COMPLETED_WITH_ERRORS);
    }

    @Test
    void shouldSkipAlreadyMigratedRecords() {
        // Given
        when(legacyRepository.findAll()).thenReturn(Arrays.asList(validLegacyRecord));

        // Mock existing combination with same name and color
        ColorCombination existingCombination = new ColorCombination("Valid Color");
        existingCombination.setColorCount(1);
        ColorInCombination existingColor = new ColorInCombination("FF0000", 1);
        existingCombination.addColor(existingColor);

        when(colorCombinationRepository.findByNameContainingIgnoreCase("Valid Color"))
                .thenReturn(Arrays.asList(existingCombination));

        when(colorCombinationRepository.findByColorCount(1))
                .thenReturn(Arrays.asList(existingCombination));
        when(legacyRepository.count()).thenReturn(1L);

        // When
        MigrationResult result = migrationService.migrateLegacyData();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalLegacyRecords()).isEqualTo(1);
        assertThat(result.getMigratedRecords()).isEqualTo(1); // Considered successful
        assertThat(result.getFailedRecords()).isEqualTo(0);
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).contains("already migrated");

        // Should not save new records
        verify(colorCombinationRepository, never()).save(any());
    }

    @Test
    void shouldCheckLegacyDataPresence() {
        // Given
        when(legacyRepository.count()).thenReturn(5L);

        // When
        boolean hasLegacyData = migrationService.isLegacyDataPresent();

        // Then
        assertThat(hasLegacyData).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNoLegacyDataPresent() {
        // Given
        when(legacyRepository.count()).thenReturn(0L);

        // When
        boolean hasLegacyData = migrationService.isLegacyDataPresent();

        // Then
        assertThat(hasLegacyData).isFalse();
    }

    @Test
    void shouldHandleExceptionWhenCheckingLegacyData() {
        // Given
        when(legacyRepository.count()).thenThrow(new RuntimeException("Database error"));

        // When
        boolean hasLegacyData = migrationService.isLegacyDataPresent();

        // Then
        assertThat(hasLegacyData).isFalse();
    }

    @Test
    void shouldReturnCorrectMigrationStatus() {
        // Initially not started
        assertThat(migrationService.getMigrationStatus()).isEqualTo(MigrationStatus.NOT_STARTED);

        // After successful migration
        when(legacyRepository.findAll()).thenReturn(Collections.emptyList());
        migrationService.migrateLegacyData();
        assertThat(migrationService.getMigrationStatus()).isEqualTo(MigrationStatus.NO_LEGACY_DATA);
    }

    @Test
    void shouldReturnLastMigrationResult() {
        // Initially no result
        assertThat(migrationService.getLastMigrationResult()).isEmpty();

        // After migration
        when(legacyRepository.findAll()).thenReturn(Collections.emptyList());
        MigrationResult result = migrationService.migrateLegacyData();

        Optional<MigrationResult> lastResult = migrationService.getLastMigrationResult();
        assertThat(lastResult).isPresent();
        assertThat(lastResult.get()).isEqualTo(result);
    }

    @Test
    void shouldResetMigrationStatus() {
        // Perform migration first
        when(legacyRepository.findAll()).thenReturn(Collections.emptyList());
        migrationService.migrateLegacyData();
        assertThat(migrationService.getMigrationStatus()).isEqualTo(MigrationStatus.NO_LEGACY_DATA);

        // Reset
        migrationService.resetMigrationStatus();
        assertThat(migrationService.getMigrationStatus()).isEqualTo(MigrationStatus.NOT_STARTED);
        assertThat(migrationService.getLastMigrationResult()).isEmpty();
    }

    @Test
    void shouldReturnMigrationStatistics() {
        // Given
        when(legacyRepository.count()).thenReturn(10L);
        when(colorCombinationRepository.findByColorCount(1)).thenReturn(Arrays.asList(
                new ColorCombination("Test1"),
                new ColorCombination("Test2")
        ));
        when(colorCombinationRepository.count()).thenReturn(5L);

        // When
        MigrationStatistics stats = migrationService.getMigrationStatistics();

        // Then
        assertThat(stats.getLegacyRecordCount()).isEqualTo(10);
        assertThat(stats.getSingleColorCombinationCount()).isEqualTo(2);
        assertThat(stats.getTotalCombinationCount()).isEqualTo(5);
        assertThat(stats.getMigrationStatus()).isEqualTo(MigrationStatus.NOT_STARTED);
    }

    @Test
    void shouldCreateBackupSuccessfully() {
        // Given
        when(legacyRepository.findAll()).thenReturn(Arrays.asList(validLegacyRecord));

        // When
        boolean backupCreated = migrationService.createBackup();

        // Then
        assertThat(backupCreated).isTrue();
        verify(legacyRepository).findAll();
    }

    @Test
    void shouldHandleBackupFailure() {
        // Given
        when(legacyRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // When
        boolean backupCreated = migrationService.createBackup();

        // Then
        assertThat(backupCreated).isFalse();
    }

    @Test
    void shouldValidateRecordWithNullName() {
        // Given
        KolorKombination recordWithNullName = new KolorKombination(null, "FF0000");
        when(legacyRepository.findAll()).thenReturn(Arrays.asList(recordWithNullName));

        // When
        MigrationResult result = migrationService.migrateLegacyData();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("name is null or empty"));
    }

    @Test
    void shouldValidateRecordWithShortName() {
        // Given
        KolorKombination recordWithShortName = new KolorKombination("AB", "FF0000");
        when(legacyRepository.findAll()).thenReturn(Arrays.asList(recordWithShortName));

        // When
        MigrationResult result = migrationService.migrateLegacyData();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("name too short"));
    }

    @Test
    void shouldValidateRecordWithInvalidHex() {
        // Given
        KolorKombination recordWithInvalidHex = new KolorKombination("Valid Name", "GGGGGG");
        when(legacyRepository.findAll()).thenReturn(Arrays.asList(recordWithInvalidHex));

        // When
        MigrationResult result = migrationService.migrateLegacyData();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("invalid hex value"));
    }

    @Test
    void shouldPerformPostMigrationValidation() {
        // Given
        when(legacyRepository.findAll()).thenReturn(Arrays.asList(validLegacyRecord));
        when(colorCombinationRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(Collections.emptyList());

        ColorCombination savedCombination = new ColorCombination("Valid Color");
        savedCombination.setId(1L);
        ColorInCombination color = new ColorInCombination("FF0000", 1);
        savedCombination.addColor(color);

        when(colorCombinationRepository.save(any(ColorCombination.class)))
                .thenReturn(savedCombination);
        when(colorCombinationRepository.findByColorCount(1))
                .thenReturn(Arrays.asList(savedCombination));
        when(legacyRepository.count()).thenReturn(1L);

        // When
        MigrationResult result = migrationService.migrateLegacyData();

        // Then
        assertThat(result.isSuccess()).isTrue();
        // Post-migration validation should have been performed
        verify(legacyRepository, atLeastOnce()).count();
        verify(colorCombinationRepository, atLeastOnce()).findByColorCount(1);
    }
}
