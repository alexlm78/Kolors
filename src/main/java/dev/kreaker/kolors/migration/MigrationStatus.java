package dev.kreaker.kolors.migration;

/** Enumeration representing the status of the migration process */
public enum MigrationStatus {

    /** No migration has been performed yet */
    NOT_STARTED("Migration not started"),
    /** Migration is currently in progress */
    IN_PROGRESS("Migration in progress"),
    /** Migration completed successfully */
    COMPLETED("Migration completed successfully"),
    /** Migration completed with some errors */
    COMPLETED_WITH_ERRORS("Migration completed with errors"),
    /** Migration failed completely */
    FAILED("Migration failed"),
    /** No legacy data found to migrate */
    NO_LEGACY_DATA("No legacy data found");

    private final String description;

    MigrationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return this == COMPLETED || this == COMPLETED_WITH_ERRORS;
    }

    public boolean isInProgress() {
        return this == IN_PROGRESS;
    }

    public boolean hasErrors() {
        return this == COMPLETED_WITH_ERRORS || this == FAILED;
    }

    @Override
    public String toString() {
        return description;
    }
}
