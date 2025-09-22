package dev.kreaker.kolors.aspect;

import java.time.Duration;
import java.time.Instant;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import dev.kreaker.kolors.service.PerformanceMonitoringService;

/**
 * Aspect for monitoring performance of service methods and repository
 * operations Automatically tracks execution times and logs performance metrics
 */
@Aspect
@Component
@Profile("!test")
public class PerformanceMonitoringAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

    private final PerformanceMonitoringService performanceMonitoringService;

    public PerformanceMonitoringAspect(PerformanceMonitoringService performanceMonitoringService) {
        this.performanceMonitoringService = performanceMonitoringService;
    }

    /**
     * Monitor all service method executions (excluding
     * PerformanceMonitoringService to avoid recursion)
     */
    @Around("execution(* dev.kreaker.kolors.service.*.*(..)) && !execution(* dev.kreaker.kolors.service.PerformanceMonitoringService.*(..))")
    public Object monitorServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName();

        Instant start = Instant.now();
        try {
            Object result = joinPoint.proceed();
            Duration executionTime = Duration.between(start, Instant.now());

            performanceMonitoringService.recordServiceMethod(methodName, executionTime);

            return result;
        } catch (Exception e) {
            Duration executionTime = Duration.between(start, Instant.now());
            logger.warn("Service method '{}' failed after {}ms: {}", methodName, executionTime.toMillis(), e.getMessage());
            throw e;
        }
    }

    /**
     * Monitor repository method executions (database operations)
     */
    @Around("execution(* dev.kreaker.kolors.*Repository.*(..))")
    public Object monitorRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String operationName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName();

        Instant start = Instant.now();
        try {
            Object result = joinPoint.proceed();
            Duration executionTime = Duration.between(start, Instant.now());

            performanceMonitoringService.recordDatabaseOperation(operationName, executionTime);

            return result;
        } catch (Exception e) {
            Duration executionTime = Duration.between(start, Instant.now());
            logger.warn("Database operation '{}' failed after {}ms: {}", operationName, executionTime.toMillis(), e.getMessage());
            throw e;
        }
    }

    /**
     * Monitor controller method executions for web request performance
     */
    @Around("execution(* dev.kreaker.kolors.*Controller.*(..))")
    public Object monitorControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName();

        Instant start = Instant.now();
        try {
            Object result = joinPoint.proceed();
            Duration executionTime = Duration.between(start, Instant.now());

            // Log controller performance for web request monitoring
            if (executionTime.toMillis() > 200) { // Log if request takes more than 200ms
                logger.info("Controller method '{}' took {}ms", methodName, executionTime.toMillis());
            }

            return result;
        } catch (Exception e) {
            Duration executionTime = Duration.between(start, Instant.now());
            logger.warn("Controller method '{}' failed after {}ms: {}", methodName, executionTime.toMillis(), e.getMessage());
            throw e;
        }
    }
}
