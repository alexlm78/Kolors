package dev.kreaker.kolors.migration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class MigrationStatusTest {

    @Test
    void shouldHaveCorrectDescriptions() {
        assertThat(MigrationStatus.NOT_STARTED.getDescription()).isEqualTo("Migration not started");
        assertThat(MigrationStatus.IN_PROGRESS.getDescription()).isEqualTo("Migration in progress");
        assertThat(MigrationStatus.COMPLETED.getDescription()).isEqualTo("Migration completed successfully");
        assertThat(MigrationStatus.COMPLETED_WITH_ERRORS.getDescription()).isEqualTo("Migration completed with errors");
        assertThat(MigrationStatus.FAILED.getDescription()).isEqualTo("Migration failed");
        assertThat(MigrationStatus.NO_LEGACY_DATA.getDescription()).isEqualTo("No legacy data found");
    }

    @Test
    void shouldIdentifyCompletedStatuses() {
        assertThat(MigrationStatus.COMPLETED.isCompleted()).isTrue();
        assertThat(MigrationStatus.COMPLETED_WITH_ERRORS.isCompleted()).isTrue();

        assertThat(MigrationStatus.NOT_STARTED.isCompleted()).isFalse();
        assertThat(MigrationStatus.IN_PROGRESS.isCompleted()).isFalse();
        assertThat(MigrationStatus.FAILED.isCompleted()).isFalse();
        assertThat(MigrationStatus.NO_LEGACY_DATA.isCompleted()).isFalse();
    }

    @Test
    void shouldIdentifyInProgressStatus() {
        assertThat(MigrationStatus.IN_PROGRESS.isInProgress()).isTrue();

        assertThat(MigrationStatus.NOT_STARTED.isInProgress()).isFalse();
        assertThat(MigrationStatus.COMPLETED.isInProgress()).isFalse();
        assertThat(MigrationStatus.COMPLETED_WITH_ERRORS.isInProgress()).isFalse();
        assertThat(MigrationStatus.FAILED.isInProgress()).isFalse();
        assertThat(MigrationStatus.NO_LEGACY_DATA.isInProgress()).isFalse();
    }

    @Test
    void shouldIdentifyErrorStatuses() {
        assertThat(MigrationStatus.COMPLETED_WITH_ERRORS.hasErrors()).isTrue();
        assertThat(MigrationStatus.FAILED.hasErrors()).isTrue();

        assertThat(MigrationStatus.NOT_STARTED.hasErrors()).isFalse();
        assertThat(MigrationStatus.IN_PROGRESS.hasErrors()).isFalse();
        assertThat(MigrationStatus.COMPLETED.hasErrors()).isFalse();
        assertThat(MigrationStatus.NO_LEGACY_DATA.hasErrors()).isFalse();
    }

    @Test
    void shouldHaveCorrectToString() {
        assertThat(MigrationStatus.NOT_STARTED.toString()).isEqualTo("Migration not started");
        assertThat(MigrationStatus.COMPLETED.toString()).isEqualTo("Migration completed successfully");
    }
}
