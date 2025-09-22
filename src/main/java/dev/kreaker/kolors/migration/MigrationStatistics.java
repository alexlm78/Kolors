package dev.kreaker.kolors.migration;

import java.time.LocalDateTime;

/** Statistics about the migration process and current state */
public class MigrationStatistics {

  private long legacyRecordCount;
  private long singleColorCombinationCount;
  private long totalCombinationCount;
  private MigrationStatus migrationStatus;
  private LocalDateTime lastMigrationTime;
  private boolean lastMigrationSuccess;

  public MigrationStatistics() {
    this.migrationStatus = MigrationStatus.NOT_STARTED;
  }

  // Getters and Setters
  public long getLegacyRecordCount() {
    return legacyRecordCount;
  }

  public void setLegacyRecordCount(long legacyRecordCount) {
    this.legacyRecordCount = legacyRecordCount;
  }

  public long getSingleColorCombinationCount() {
    return singleColorCombinationCount;
  }

  public void setSingleColorCombinationCount(long singleColorCombinationCount) {
    this.singleColorCombinationCount = singleColorCombinationCount;
  }

  public long getTotalCombinationCount() {
    return totalCombinationCount;
  }

  public void setTotalCombinationCount(long totalCombinationCount) {
    this.totalCombinationCount = totalCombinationCount;
  }

  public MigrationStatus getMigrationStatus() {
    return migrationStatus;
  }

  public void setMigrationStatus(MigrationStatus migrationStatus) {
    this.migrationStatus = migrationStatus;
  }

  public LocalDateTime getLastMigrationTime() {
    return lastMigrationTime;
  }

  public void setLastMigrationTime(LocalDateTime lastMigrationTime) {
    this.lastMigrationTime = lastMigrationTime;
  }

  public boolean isLastMigrationSuccess() {
    return lastMigrationSuccess;
  }

  public void setLastMigrationSuccess(boolean lastMigrationSuccess) {
    this.lastMigrationSuccess = lastMigrationSuccess;
  }

  // Helper methods
  public boolean isMigrationNeeded() {
    return legacyRecordCount > 0 && migrationStatus == MigrationStatus.NOT_STARTED;
  }

  public boolean isMigrationComplete() {
    return migrationStatus.isCompleted();
  }

  public double getMigrationProgress() {
    if (legacyRecordCount == 0) {
      return 100.0;
    }
    return (double) singleColorCombinationCount / legacyRecordCount * 100.0;
  }

  @Override
  public String toString() {
    return "MigrationStatistics{"
        + "legacyRecordCount="
        + legacyRecordCount
        + ", singleColorCombinationCount="
        + singleColorCombinationCount
        + ", totalCombinationCount="
        + totalCombinationCount
        + ", migrationStatus="
        + migrationStatus
        + ", lastMigrationTime="
        + lastMigrationTime
        + ", lastMigrationSuccess="
        + lastMigrationSuccess
        + ", progress="
        + String.format("%.1f%%", getMigrationProgress())
        + '}';
  }
}
