/* (c) 2026 Alejandro Lopez Monzon <alejandro@kreaker.dev> for Kreaker Developments */
package dev.kreaker.kolors.controller.api;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.kreaker.kolors.service.PerformanceMonitoringService;
import dev.kreaker.kolors.service.PerformanceMonitoringService.DatabasePerformanceSummary;
import dev.kreaker.kolors.service.PerformanceMonitoringService.PerformanceMetric;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for performance monitoring and metrics Provides endpoints to view and manage
 * performance data
 */
@RestController
@RequestMapping("/api/performance")
@Tag(name = "Performance Monitoring",
         description = "API for monitoring application performance and database metrics")
public class PerformanceController {

   private final PerformanceMonitoringService performanceMonitoringService;

   public PerformanceController(PerformanceMonitoringService performanceMonitoringService) {
      this.performanceMonitoringService = performanceMonitoringService;
   }

   /** Gets database performance summary */
   @Operation(summary = "Get database performance summary",
            description = "Retrieves a summary of database query performance metrics")
   @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Successfully retrieved database performance summary",
            content = @Content(mediaType = "application/json",
                     schema = @Schema(implementation = DatabasePerformanceSummary.class)))})
   @GetMapping("/database")
   public ResponseEntity<DatabasePerformanceSummary> getDatabasePerformance() {
      DatabasePerformanceSummary summary =
               performanceMonitoringService.getDatabasePerformanceSummary();
      return ResponseEntity.ok(summary);
   }

   /** Gets all performance metrics */
   @Operation(summary = "Get all performance metrics",
            description = "Retrieves all collected performance metrics for the application")
   @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all metrics",
                     content = @Content(mediaType = "application/json",
                              schema = @Schema(implementation = Map.class)))})
   @GetMapping("/metrics")
   public ResponseEntity<Map<String, PerformanceMetric>> getAllMetrics() {
      Map<String, PerformanceMetric> metrics = performanceMonitoringService.getAllMetrics();
      return ResponseEntity.ok(metrics);
   }

   /** Gets performance metrics for a specific operation */
   @Operation(summary = "Get metrics for a specific operation",
            description = "Retrieves performance metrics for a specific named operation")
   @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                     description = "Successfully retrieved operation metrics",
                     content = @Content(mediaType = "application/json",
                              schema = @Schema(implementation = PerformanceMetric.class))),
            @ApiResponse(responseCode = "404", description = "Operation metric not found")})
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
   @Operation(summary = "Reset all metrics",
            description = "Clears all collected performance metrics")
   @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics reset successfully")})
   @PostMapping("/reset")
   public ResponseEntity<String> resetMetrics() {
      performanceMonitoringService.resetMetrics();
      return ResponseEntity.ok("Performance metrics reset successfully");
   }

   /** Logs current performance summary */
   @Operation(summary = "Log performance summary",
            description = "Triggers logging of the current performance summary to the application logs")
   @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Performance summary logged successfully")})
   @PostMapping("/log-summary")
   public ResponseEntity<String> logPerformanceSummary() {
      performanceMonitoringService.logPerformanceSummary();
      return ResponseEntity.ok("Performance summary logged");
   }
}
