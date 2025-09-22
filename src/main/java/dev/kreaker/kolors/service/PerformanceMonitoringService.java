package dev.kreaker.kolors.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for monitoring application performance metrics Tracks query execution times, method
 * calls, and database operations
 */
@Service
public class PerformanceMonitoringService {

  private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);
  private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");

  // Metrics storage
  private final ConcurrentHashMap<String, PerformanceMetric> metrics = new ConcurrentHashMap<>();
  private final AtomicLong totalQueries = new AtomicLong(0);
  private final AtomicLong slowQueries = new AtomicLong(0);

  // Thresholds
  private static final long SLOW_QUERY_THRESHOLD_MS = 1000; // 1 second
  private static final long WARNING_QUERY_THRESHOLD_MS = 500; // 500ms

  /** Records the execution time of a database operation */
  public void recordDatabaseOperation(String operationName, Duration executionTime) {
    try {
      long executionTimeMs = executionTime.toMillis();

      // Update counters
      totalQueries.incrementAndGet();

      // Check if it's a slow query
      if (executionTimeMs > SLOW_QUERY_THRESHOLD_MS) {
        slowQueries.incrementAndGet();
        performanceLogger.warn("SLOW QUERY detected: {} took {}ms", operationName, executionTimeMs);
      } else if (executionTimeMs > WARNING_QUERY_THRESHOLD_MS) {
        performanceLogger.info(
            "Query performance warning: {} took {}ms", operationName, executionTimeMs);
      }

      // Update metrics
      metrics.compute(
          operationName,
          (key, existing) -> {
            if (existing == null) {
              return new PerformanceMetric(operationName, executionTimeMs);
            } else {
              existing.addExecution(executionTimeMs);
              return existing;
            }
          });

      logger.debug("Database operation '{}' completed in {}ms", operationName, executionTimeMs);
    } catch (Exception e) {
      // Avoid recursion by not logging through the monitored logger
      System.err.println("Error recording database operation: " + e.getMessage());
    }
  }

  /** Records the execution time of a service method */
  public void recordServiceMethod(String methodName, Duration executionTime) {
    long executionTimeMs = executionTime.toMillis();

    if (executionTimeMs > WARNING_QUERY_THRESHOLD_MS) {
      performanceLogger.info(
          "Service method performance: {} took {}ms", methodName, executionTimeMs);
    }

    metrics.compute(
        "service." + methodName,
        (key, existing) -> {
          if (existing == null) {
            return new PerformanceMetric("service." + methodName, executionTimeMs);
          } else {
            existing.addExecution(executionTimeMs);
            return existing;
          }
        });

    logger.debug("Service method '{}' completed in {}ms", methodName, executionTimeMs);
  }

  /** Gets performance statistics for a specific operation */
  public PerformanceMetric getMetrics(String operationName) {
    return metrics.get(operationName);
  }

  /** Gets all performance metrics */
  public ConcurrentHashMap<String, PerformanceMetric> getAllMetrics() {
    return new ConcurrentHashMap<>(metrics);
  }

  /** Gets database performance summary */
  public DatabasePerformanceSummary getDatabasePerformanceSummary() {
    return new DatabasePerformanceSummary(
        totalQueries.get(),
        slowQueries.get(),
        calculateAverageQueryTime(),
        getSlowQueryPercentage());
  }

  /** Resets all performance metrics */
  public void resetMetrics() {
    metrics.clear();
    totalQueries.set(0);
    slowQueries.set(0);
    logger.info("Performance metrics reset");
  }

  /** Logs current performance summary */
  public void logPerformanceSummary() {
    DatabasePerformanceSummary summary = getDatabasePerformanceSummary();

    performanceLogger.info("=== PERFORMANCE SUMMARY ===");
    performanceLogger.info("Total queries: {}", summary.getTotalQueries());
    performanceLogger.info(
        "Slow queries: {} ({}%)", summary.getSlowQueries(), summary.getSlowQueryPercentage());
    performanceLogger.info("Average query time: {}ms", summary.getAverageQueryTimeMs());

    // Log top 5 slowest operations
    metrics.entrySet().stream()
        .sorted(
            (e1, e2) ->
                Double.compare(e2.getValue().getAverageTimeMs(), e1.getValue().getAverageTimeMs()))
        .limit(5)
        .forEach(
            entry -> {
              PerformanceMetric metric = entry.getValue();
              performanceLogger.info(
                  "Operation '{}': avg={}ms, max={}ms, count={}",
                  entry.getKey(),
                  String.format("%.2f", metric.getAverageTimeMs()),
                  metric.getMaxTimeMs(),
                  metric.getExecutionCount());
            });

    performanceLogger.info("=== END PERFORMANCE SUMMARY ===");
  }

  private double calculateAverageQueryTime() {
    return metrics.values().stream()
        .filter(metric -> !metric.getOperationName().startsWith("service."))
        .mapToDouble(PerformanceMetric::getAverageTimeMs)
        .average()
        .orElse(0.0);
  }

  private double getSlowQueryPercentage() {
    long total = totalQueries.get();
    if (total == 0) {
      return 0.0;
    }
    return (slowQueries.get() * 100.0) / total;
  }

  /** Performance metric for a specific operation */
  public static class PerformanceMetric {

    private final String operationName;
    private long totalTimeMs;
    private long executionCount;
    private long minTimeMs;
    private long maxTimeMs;
    private final LocalDateTime firstExecution;
    private LocalDateTime lastExecution;

    public PerformanceMetric(String operationName, long executionTimeMs) {
      this.operationName = operationName;
      this.totalTimeMs = executionTimeMs;
      this.executionCount = 1;
      this.minTimeMs = executionTimeMs;
      this.maxTimeMs = executionTimeMs;
      this.firstExecution = LocalDateTime.now();
      this.lastExecution = LocalDateTime.now();
    }

    public synchronized void addExecution(long executionTimeMs) {
      this.totalTimeMs += executionTimeMs;
      this.executionCount++;
      this.minTimeMs = Math.min(this.minTimeMs, executionTimeMs);
      this.maxTimeMs = Math.max(this.maxTimeMs, executionTimeMs);
      this.lastExecution = LocalDateTime.now();
    }

    public String getOperationName() {
      return operationName;
    }

    public double getAverageTimeMs() {
      return executionCount > 0 ? (double) totalTimeMs / executionCount : 0.0;
    }

    public long getTotalTimeMs() {
      return totalTimeMs;
    }

    public long getExecutionCount() {
      return executionCount;
    }

    public long getMinTimeMs() {
      return minTimeMs;
    }

    public long getMaxTimeMs() {
      return maxTimeMs;
    }

    public LocalDateTime getFirstExecution() {
      return firstExecution;
    }

    public LocalDateTime getLastExecution() {
      return lastExecution;
    }
  }

  /** Summary of database performance metrics */
  public static class DatabasePerformanceSummary {

    private final long totalQueries;
    private final long slowQueries;
    private final double averageQueryTimeMs;
    private final double slowQueryPercentage;

    public DatabasePerformanceSummary(
        long totalQueries,
        long slowQueries,
        double averageQueryTimeMs,
        double slowQueryPercentage) {
      this.totalQueries = totalQueries;
      this.slowQueries = slowQueries;
      this.averageQueryTimeMs = averageQueryTimeMs;
      this.slowQueryPercentage = slowQueryPercentage;
    }

    public long getTotalQueries() {
      return totalQueries;
    }

    public long getSlowQueries() {
      return slowQueries;
    }

    public double getAverageQueryTimeMs() {
      return averageQueryTimeMs;
    }

    public double getSlowQueryPercentage() {
      return slowQueryPercentage;
    }
  }
}
