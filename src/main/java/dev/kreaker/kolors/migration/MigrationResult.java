package dev.kreaker.kolors.migration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Represents the result of a migration operation */
public class MigrationResult {

  private boolean success;
  private int totalLegacyRecords;
  private int migratedRecords;
  private int failedRecords;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private List<String> errors;
  private List<String> warnings;
  private String summary;

  public MigrationResult() {
    this.errors = new ArrayList<>();
    this.warnings = new ArrayList<>();
    this.startTime = LocalDateTime.now();
  }

  public MigrationResult(boolean success, int totalLegacyRecords, int migratedRecords) {
    this();
    this.success = success;
    this.totalLegacyRecords = totalLegacyRecords;
    this.migratedRecords = migratedRecords;
    this.failedRecords = totalLegacyRecords - migratedRecords;
    this.endTime = LocalDateTime.now();
  }

  // Getters and Setters
  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public int getTotalLegacyRecords() {
    return totalLegacyRecords;
  }

  public void setTotalLegacyRecords(int totalLegacyRecords) {
    this.totalLegacyRecords = totalLegacyRecords;
  }

  public int getMigratedRecords() {
    return migratedRecords;
  }

  public void setMigratedRecords(int migratedRecords) {
    this.migratedRecords = migratedRecords;
  }

  public int getFailedRecords() {
    return failedRecords;
  }

  public void setFailedRecords(int failedRecords) {
    this.failedRecords = failedRecords;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  // Helper methods
  public void addError(String error) {
    this.errors.add(error);
  }

  public void addWarning(String warning) {
    this.warnings.add(warning);
  }

  public void complete() {
    this.endTime = LocalDateTime.now();
    generateSummary();
  }

  public long getDurationInSeconds() {
    if (startTime != null && endTime != null) {
      return java.time.Duration.between(startTime, endTime).getSeconds();
    }
    return 0;
  }

  private void generateSummary() {
    StringBuilder sb = new StringBuilder();
    sb.append("Migration completed ");
    sb.append(success ? "successfully" : "with errors");
    sb.append(". Total records: ").append(totalLegacyRecords);
    sb.append(", Migrated: ").append(migratedRecords);
    sb.append(", Failed: ").append(failedRecords);
    sb.append(", Duration: ").append(getDurationInSeconds()).append(" seconds");

    if (!errors.isEmpty()) {
      sb.append(", Errors: ").append(errors.size());
    }
    if (!warnings.isEmpty()) {
      sb.append(", Warnings: ").append(warnings.size());
    }

    this.summary = sb.toString();
  }

  @Override
  public String toString() {
    return "MigrationResult{"
        + "success="
        + success
        + ", totalLegacyRecords="
        + totalLegacyRecords
        + ", migratedRecords="
        + migratedRecords
        + ", failedRecords="
        + failedRecords
        + ", duration="
        + getDurationInSeconds()
        + "s"
        + ", errors="
        + errors.size()
        + ", warnings="
        + warnings.size()
        + '}';
  }
}
