package dev.kreaker.kolors.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Scheduled service for periodic performance monitoring tasks Logs performance
 * summaries and monitors system health
 */
@Service
@Profile("!test")
public class PerformanceScheduledService {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceScheduledService.class);

    private final PerformanceMonitoringService performanceMonitoringService;

    public PerformanceScheduledService(PerformanceMonitoringService performanceMonitoringService) {
        this.performanceMonitoringService = performanceMonitoringService;
    }

    /**
     * Logs performance summary every 5 minutes
     */
    // @Scheduled(fixedRate = 300000) // 5 minutes - Disabled for testing
    public void logPerformanceSummary() {
        try {
            performanceMonitoringService.logPerformanceSummary();
        } catch (Exception e) {
            logger.error("Error logging performance summary", e);
        }
    }

    /**
     * Resets performance metrics daily at midnight
     */
    // @Scheduled(cron = "0 0 0 * * *") // Daily at midnight - Disabled for testing
    public void resetDailyMetrics() {
        try {
            logger.info("Resetting daily performance metrics");
            performanceMonitoringService.resetMetrics();
        } catch (Exception e) {
            logger.error("Error resetting daily metrics", e);
        }
    }
}
