package dev.kreaker.kolors.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MigrationResultTest {

    private MigrationResult migrationResult;

    @BeforeEach
    void setUp() {
        migrationResult = new MigrationResult();
    }

    @Test
    void shouldInitializeWithDefaults() {
        assertThat(migrationResult.isSuccess()).isFalse();
        assertThat(migrationResult.getTotalLegacyRecords()).isEqualTo(0);
        assertThat(migrationResult.getMigratedRecords()).isEqualTo(0);
        assertThat(migrationResult.getFailedRecords()).isEqualTo(0);
        assertThat(migrationResult.getErrors()).isEmpty();
        assertThat(migrationResult.getWarnings()).isEmpty();
        assertThat(migrationResult.getStartTime()).isNotNull();
        assertThat(migrationResult.getEndTime()).isNull();
    }

    @Test
    void shouldCreateWithParameters() {
        MigrationResult result = new MigrationResult(true, 10, 8);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalLegacyRecords()).isEqualTo(10);
        assertThat(result.getMigratedRecords()).isEqualTo(8);
        assertThat(result.getFailedRecords()).isEqualTo(2);
        assertThat(result.getEndTime()).isNotNull();
    }

    @Test
    void shouldAddErrorsAndWarnings() {
        migrationResult.addError("Test error");
        migrationResult.addWarning("Test warning");

        assertThat(migrationResult.getErrors()).containsExactly("Test error");
        assertThat(migrationResult.getWarnings()).containsExactly("Test warning");
    }

    @Test
    void shouldCompleteAndGenerateSummary() {
        migrationResult.setSuccess(true);
        migrationResult.setTotalLegacyRecords(5);
        migrationResult.setMigratedRecords(4);
        migrationResult.setFailedRecords(1);
        migrationResult.addError("Sample error");
        migrationResult.addWarning("Sample warning");

        migrationResult.complete();

        assertThat(migrationResult.getEndTime()).isNotNull();
        assertThat(migrationResult.getSummary()).isNotNull();
        assertThat(migrationResult.getSummary()).contains("Migration completed successfully");
        assertThat(migrationResult.getSummary()).contains("Total records: 5");
        assertThat(migrationResult.getSummary()).contains("Migrated: 4");
        assertThat(migrationResult.getSummary()).contains("Failed: 1");
        assertThat(migrationResult.getSummary()).contains("Errors: 1");
        assertThat(migrationResult.getSummary()).contains("Warnings: 1");
    }

    @Test
    void shouldCalculateDuration() {
        LocalDateTime start = LocalDateTime.now().minusSeconds(30);
        migrationResult.setStartTime(start);
        migrationResult.setEndTime(start.plusSeconds(30));

        assertThat(migrationResult.getDurationInSeconds()).isEqualTo(30);
    }

    @Test
    void shouldReturnZeroDurationWhenTimesNotSet() {
        migrationResult.setStartTime(null);
        migrationResult.setEndTime(null);

        assertThat(migrationResult.getDurationInSeconds()).isEqualTo(0);
    }

    @Test
    void shouldGenerateFailureSummary() {
        migrationResult.setSuccess(false);
        migrationResult.setTotalLegacyRecords(3);
        migrationResult.setMigratedRecords(0);
        migrationResult.setFailedRecords(3);

        migrationResult.complete();

        assertThat(migrationResult.getSummary()).contains("Migration completed with errors");
    }

    @Test
    void shouldHaveProperToString() {
        migrationResult.setSuccess(true);
        migrationResult.setTotalLegacyRecords(10);
        migrationResult.setMigratedRecords(9);
        migrationResult.setFailedRecords(1);
        migrationResult.addError("Test error");

        String toString = migrationResult.toString();

        assertThat(toString).contains("success=true");
        assertThat(toString).contains("totalLegacyRecords=10");
        assertThat(toString).contains("migratedRecords=9");
        assertThat(toString).contains("failedRecords=1");
        assertThat(toString).contains("errors=1");
    }
}
