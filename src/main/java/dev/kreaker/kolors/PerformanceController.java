package dev.kreaker.kolors;

import dev.kreaker.kolors.service.PerformanceMonitoringService;
import dev.kreaker.kolors.service.PerformanceMonitoringService.DatabasePerformanceSummary;
import dev.kreaker.kolors.service.PerformanceMonitoringService.PerformanceMetric;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for performance monitoring and metrics Provides endpoints to view and manage
 * performance data
 */
@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final PerformanceMonitoringService performanceMonitoringService;

    public PerformanceController(PerformanceMonitoringService performanceMonitoringService) {
        this.performanceMonitoringService = performanceMonitoringService;
    }

    /** Gets database performance summary */
    @GetMapping("/database")
    public ResponseEntity<DatabasePerformanceSummary> getDatabasePerformance() {
        DatabasePerformanceSummary summary =
                performanceMonitoringService.getDatabasePerformanceSummary();
        return ResponseEntity.ok(summary);
    }

    /** Gets all performance metrics */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, PerformanceMetric>> getAllMetrics() {
        Map<String, PerformanceMetric> metrics = performanceMonitoringService.getAllMetrics();
        return ResponseEntity.ok(metrics);
    }

    /** Gets performance metrics for a specific operation */
    @GetMapping("/metrics/{operationName}")
    public ResponseEntity<PerformanceMetric> getOperationMetrics(String operationName) {
        PerformanceMetric metric = performanceMonitoringService.getMetrics(operationName);
        if (metric != null) {
            return ResponseEntity.ok(metric);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /** Resets all performance metrics */
    @PostMapping("/reset")
    public ResponseEntity<String> resetMetrics() {
        performanceMonitoringService.resetMetrics();
        return ResponseEntity.ok("Performance metrics reset successfully");
    }

    /** Logs current performance summary */
    @PostMapping("/log-summary")
    public ResponseEntity<String> logPerformanceSummary() {
        performanceMonitoringService.logPerformanceSummary();
        return ResponseEntity.ok("Performance summary logged");
    }
}
