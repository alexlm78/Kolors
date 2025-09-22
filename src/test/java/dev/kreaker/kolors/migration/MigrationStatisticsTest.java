package dev.kreaker.kolors.migration;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MigrationStatisticsTest {

    private MigrationStatistics statistics;

    @BeforeEach
    void setUp() {
        statistics = new MigrationStatistics();
    }

    @Test
    void shouldInitializeWithDefaults() {
        assertThat(statistics.getLegacyRecordCount()).isEqualTo(0);
        assertThat(statistics.getSingleColorCombinationCount()).isEqualTo(0);
        assertThat(statistics.getTotalCombinationCount()).isEqualTo(0);
        assertThat(statistics.getMigrationStatus()).isEqualTo(MigrationStatus.NOT_STARTED);
        assertThat(statistics.getLastMigrationTime()).isNull();
        assertThat(statistics.isLastMigrationSuccess()).isFalse();
    }

    @Test
    void shouldSetAllProperties() {
        LocalDateTime now = LocalDateTime.now();

        statistics.setLegacyRecordCount(10);
        statistics.setSingleColorCombinationCount(8);
        statistics.setTotalCombinationCount(15);
        statistics.setMigrationStatus(MigrationStatus.COMPLETED);
        statistics.setLastMigrationTime(now);
        statistics.setLastMigrationSuccess(true);

        assertThat(statistics.getLegacyRecordCount()).isEqualTo(10);
        assertThat(statistics.getSingleColorCombinationCount()).isEqualTo(8);
        assertThat(statistics.getTotalCombinationCount()).isEqualTo(15);
        assertThat(statistics.getMigrationStatus()).isEqualTo(MigrationStatus.COMPLETED);
        assertThat(statistics.getLastMigrationTime()).isEqualTo(now);
        assertThat(statistics.isLastMigrationSuccess()).isTrue();
    }

    @Test
    void shouldIdentifyWhenMigrationIsNeeded() {
        statistics.setLegacyRecordCount(5);
        statistics.setMigrationStatus(MigrationStatus.NOT_STARTED);

        assertThat(statistics.isMigrationNeeded()).isTrue();
    }

    @Test
    void shouldIdentifyWhenMigrationIsNotNeeded() {
        // No legacy data
        statistics.setLegacyRecordCount(0);
        statistics.setMigrationStatus(MigrationStatus.NOT_STARTED);
        assertThat(statistics.isMigrationNeeded()).isFalse();

        // Migration already completed
        statistics.setLegacyRecordCount(5);
        statistics.setMigrationStatus(MigrationStatus.COMPLETED);
        assertThat(statistics.isMigrationNeeded()).isFalse();
    }

    @Test
    void shouldIdentifyWhenMigrationIsComplete() {
        statistics.setMigrationStatus(MigrationStatus.COMPLETED);
        assertThat(statistics.isMigrationComplete()).isTrue();

        statistics.setMigrationStatus(MigrationStatus.COMPLETED_WITH_ERRORS);
        assertThat(statistics.isMigrationComplete()).isTrue();

        statistics.setMigrationStatus(MigrationStatus.NOT_STARTED);
        assertThat(statistics.isMigrationComplete()).isFalse();
    }

    @Test
    void shouldCalculateMigrationProgress() {
        // No legacy data - should be 100%
        statistics.setLegacyRecordCount(0);
        statistics.setSingleColorCombinationCount(0);
        assertThat(statistics.getMigrationProgress()).isEqualTo(100.0);

        // Partial migration
        statistics.setLegacyRecordCount(10);
        statistics.setSingleColorCombinationCount(8);
        assertThat(statistics.getMigrationProgress()).isEqualTo(80.0);

        // Complete migration
        statistics.setLegacyRecordCount(5);
        statistics.setSingleColorCombinationCount(5);
        assertThat(statistics.getMigrationProgress()).isEqualTo(100.0);

        // Over-migration (more combinations than legacy records)
        statistics.setLegacyRecordCount(3);
        statistics.setSingleColorCombinationCount(5);
        assertThat(statistics.getMigrationProgress()).isGreaterThan(100.0);
    }

    @Test
    void shouldHaveProperToString() {
        statistics.setLegacyRecordCount(10);
        statistics.setSingleColorCombinationCount(8);
        statistics.setTotalCombinationCount(15);
        statistics.setMigrationStatus(MigrationStatus.COMPLETED);
        statistics.setLastMigrationSuccess(true);

        String toString = statistics.toString();

        assertThat(toString).contains("legacyRecordCount=10");
        assertThat(toString).contains("singleColorCombinationCount=8");
        assertThat(toString).contains("totalCombinationCount=15");
        assertThat(toString).contains("migrationStatus=" + MigrationStatus.COMPLETED);
        assertThat(toString).contains("lastMigrationSuccess=true");
        assertThat(toString).contains("progress=80.0%");
    }
}
