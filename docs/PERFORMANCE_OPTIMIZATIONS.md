# Performance Optimizations

This document describes the performance optimizations implemented in the Kolors application to improve database query performance and overall system responsiveness.

## Database Indexes

### ColorCombination Entity Indexes
- `idx_combination_name`: Index on the `name` column for fast name-based searches
- `idx_combination_created_at`: Index on the `created_at` column for chronological ordering
- `idx_combination_color_count`: Index on the `color_count` column for filtering by number of colors
- `idx_combination_name_color_count`: Composite index for combined name and color count searches

### ColorInCombination Entity Indexes
- `idx_color_hex_value`: Index on the `hex_value` column for color-based searches
- `idx_color_combination_id`: Index on the `combination_id` foreign key for join operations
- `idx_color_position`: Index on the `position` column for ordering operations
- `idx_color_combination_position`: Composite index for combination and position lookups

## EntityGraph Optimizations

### N+1 Query Prevention
The application uses `@EntityGraph` annotations to prevent N+1 query problems:

```java
@EntityGraph(attributePaths = {"colors"})
List<ColorCombination> findByNameContainingIgnoreCase(String name);
```

This ensures that when fetching combinations, their associated colors are loaded in a single query instead of separate queries for each combination.

### Optimized Repository Methods
Key repository methods have been optimized with EntityGraph:
- `findAllByOrderByCreatedAtDesc()`: Loads all combinations with colors in one query
- `findByNameContainingIgnoreCase()`: Name search with eager color loading
- `findByColorCount()`: Color count filtering with eager loading
- `findByContainingHexValue()`: Hex value search with optimized joins
- `findByIdWithColors()`: Single combination lookup with colors

## Performance Monitoring

### Automatic Performance Tracking
The application includes comprehensive performance monitoring:

```java
@Aspect
@Component
public class PerformanceMonitoringAspect {
    // Monitors service methods and repository operations
    // Tracks execution times and identifies slow queries
}
```

### Performance Metrics
- **Query Execution Times**: Tracks all database operations
- **Slow Query Detection**: Identifies queries taking >1000ms
- **Service Method Performance**: Monitors business logic execution
- **Controller Response Times**: Tracks web request performance

### Performance Endpoints
- `GET /api/performance/database`: Database performance summary
- `GET /api/performance/metrics`: All performance metrics
- `POST /api/performance/reset`: Reset performance counters
- `POST /api/performance/log-summary`: Log current performance summary

## Database Configuration Optimizations

### Connection Pool Settings
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
```

### JPA Performance Settings
```properties
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
spring.jpa.properties.hibernate.generate_statistics=true
```

## Lazy Loading Strategy

### Proper Fetch Types
- `ColorCombination.colors`: Uses `FetchType.LAZY` by default
- EntityGraph used selectively for operations that need colors
- Avoids unnecessary data loading when colors aren't needed

### Transaction Boundaries
- Read-only transactions for query operations
- Proper transaction scoping to avoid lazy loading exceptions

## Query Optimization Techniques

### Strategic Query Design
1. **Index-Aware Queries**: Queries designed to utilize available indexes
2. **Selective Fetching**: Only load data that's actually needed
3. **Batch Operations**: Use batch processing for bulk operations
4. **Query Caching**: Leverage Hibernate's query cache where appropriate

### Search Optimization
- Name searches use case-insensitive LIKE with indexes
- Color count filtering uses direct equality with indexes
- Hex value searches use optimized joins with color indexes
- Combined searches use composite indexes where possible

## Performance Testing

### Automated Tests
The `DatabaseIndexTest` class verifies:
- EntityGraph optimizations work correctly
- Indexes are being utilized
- Search operations perform efficiently
- No N+1 query problems exist

### Performance Monitoring
- Continuous monitoring of query performance
- Automatic detection of performance regressions
- Logging of slow queries for investigation
- Regular performance summaries

## Best Practices Implemented

1. **Database Design**
   - Strategic index placement
   - Proper foreign key relationships
   - Optimized data types

2. **JPA/Hibernate Usage**
   - EntityGraph for selective eager loading
   - Proper fetch strategies
   - Batch processing configuration

3. **Service Layer**
   - Read-only transactions for queries
   - Proper exception handling
   - Performance monitoring integration

4. **Monitoring and Observability**
   - Comprehensive performance metrics
   - Automatic slow query detection
   - Performance trend tracking

## Expected Performance Improvements

With these optimizations, the application should see:
- **50-80% reduction** in query execution times for list operations
- **Elimination of N+1 queries** in combination loading
- **Faster search operations** due to strategic indexing
- **Better scalability** with increased data volumes
- **Improved user experience** with faster page loads

## Monitoring and Maintenance

### Regular Performance Reviews
- Monitor performance metrics weekly
- Review slow query logs monthly
- Analyze performance trends quarterly
- Update indexes based on usage patterns

### Performance Alerts
- Automatic alerts for queries >1000ms
- Daily performance summaries
- Trend analysis for performance degradation
- Capacity planning based on metrics
