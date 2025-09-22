# Kolors - Dynamic Color Combination Manager

A modern Spring Boot web application for creating, managing, and exploring dynamic color combinations. Built with responsive design and optimized for performance.

## 🎨 Features

### Core Functionality

- **Dynamic Color Combinations**: Create combinations with 1-10 colors
- **Real-time Color Management**: Add, remove, and reorder colors dynamically
- **Advanced Search**: Search by name, hex values, color count, and date ranges
- **Responsive Design**: Optimized for desktop, tablet, and mobile devices
- **Performance Optimized**: Database indexes and query optimization for fast searches

### User Interface

- **Intuitive Color Picker**: Easy-to-use color selection interface
- **Live Preview**: Real-time preview of color combinations
- **Mobile-First Design**: Touch-friendly interface for all devices
- **Accessibility**: WCAG compliant with keyboard navigation support
- **Multi-language Support**: Spanish interface with English documentation

### Technical Features

- **RESTful API**: Complete REST API for programmatic access
- **Data Validation**: Comprehensive input validation and error handling
- **Database Migration**: Completed migration from legacy system
- **Performance Monitoring**: Built-in performance tracking and optimization
- **Comprehensive Testing**: Full test suite with integration and performance tests

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Gradle 8.0 or higher
- SQLite (included)

### Installation

1. **Clone the repository**

   ```bash
   git clone <repository-url>
   cd Kolors
   ```

2. **Build the application**

   ```bash
   ./gradlew build
   ```

3. **Run the application**

   ```bash
   ./gradlew bootRun
   ```

4. **Access the application**
   - Web Interface: http://localhost:8080
   - Color Combinations: http://localhost:8080/combinations/
   - Admin Panel: http://localhost:8080/admin/migration/status

## 📱 Usage Guide

### Creating Color Combinations

1. **Navigate to the main page** at http://localhost:8080/combinations/
2. **Click "Nueva Combinación"** to create a new combination
3. **Enter a name** for your color combination (minimum 3 characters)
4. **Add colors** using the color picker or hex input
5. **Save** your combination

### Managing Colors

- **Add Colors**: Click the "+" button to add up to 10 colors
- **Remove Colors**: Click the "×" button next to any color (minimum 1 color required)
- **Reorder Colors**: Colors are automatically positioned and can be managed through the interface
- **Edit Colors**: Click on any color to modify its hex value

### Searching and Filtering

- **Search by Name**: Use the search box to find combinations by name
- **Filter by Color Count**: Use the dropdown to filter by number of colors
- **Search by Hex**: Enter hex values to find combinations containing specific colors
- **Date Filtering**: Filter combinations by creation date

### Mobile Usage

The application is fully responsive and optimized for mobile devices:

- Touch-friendly color picker
- Swipe gestures for navigation
- Optimized layouts for small screens
- Fast loading on mobile networks

## 🛠 Development

### Project Structure

```
src/
├── main/
│   ├── java/dev/kreaker/kolors/
│   │   ├── ColorCombination.java          # Main entity
│   │   ├── ColorInCombination.java        # Color entity
│   │   ├── ColorCombinationController.java # Web controller
│   │   ├── ColorCombinationRestController.java # REST API
│   │   ├── ColorCombinationService.java   # Business logic
│   │   ├── migration/                     # Migration utilities
│   │   └── exception/                     # Custom exceptions
│   └── resources/
│       ├── templates/                     # Thymeleaf templates
│       ├── static/css/                    # Stylesheets
│       └── application.properties         # Configuration
└── test/                                  # Test suite
```

### Key Technologies

- **Backend**: Spring Boot 3.x, Spring Data JPA, Hibernate
- **Frontend**: Thymeleaf, Bootstrap 5, Responsive CSS
- **Database**: SQLite with optimized indexes
- **Testing**: JUnit 5, Spring Boot Test, MockMvc
- **Build**: Gradle with Spring Boot plugin

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ColorCombinationServiceTest

# Run with coverage
./gradlew test jacocoTestReport
```

### Database Schema

The application uses a normalized database schema:

- **color_combination**: Main combinations table
- **color_in_combination**: Individual colors within combinations
- Optimized indexes for search performance
- Foreign key constraints for data integrity

## 🔧 Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:sqlite:kolors.db
spring.jpa.hibernate.ddl-auto=update

# Logging
logging.level.dev.kreaker.kolors=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# Performance
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
```

### Environment Variables

- `SERVER_PORT`: Application port (default: 8080)
- `DATABASE_URL`: Database connection URL
- `LOG_LEVEL`: Logging level (DEBUG, INFO, WARN, ERROR)

## 📊 API Documentation

### REST Endpoints

#### Color Combinations

- `GET /api/combinations` - List all combinations
- `GET /api/combinations/{id}` - Get specific combination
- `POST /api/combinations` - Create new combination
- `PUT /api/combinations/{id}` - Update combination
- `DELETE /api/combinations/{id}` - Delete combination

#### Color Management

- `POST /api/combinations/{id}/colors` - Add color to combination
- `DELETE /api/combinations/{id}/colors/{position}` - Remove color
- `PUT /api/combinations/{id}/colors/{position}` - Update color

#### Search

- `GET /api/combinations/search?name={name}` - Search by name
- `GET /api/combinations/search?hex={hex}` - Search by hex value
- `GET /api/combinations/search?colorCount={count}` - Filter by color count

### Example API Usage

```bash
# Create a new combination
curl -X POST http://localhost:8080/api/combinations \
  -H "Content-Type: application/json" \
  -d '{"name": "Sunset Colors", "colors": [{"hexValue": "FF6B35", "position": 1}]}'

# Search combinations
curl "http://localhost:8080/api/combinations/search?name=sunset"

# Add a color to existing combination
curl -X POST http://localhost:8080/api/combinations/1/colors \
  -H "Content-Type: application/json" \
  -d '{"hexValue": "FF8C42", "position": 2}'
```

## 🚀 Deployment

### Production Build

```bash
# Create production JAR
./gradlew bootJar

# Run production build
java -jar build/libs/kolors-1.0.jar
```

### Docker Deployment

```dockerfile
FROM openjdk:17-jdk-slim
COPY build/libs/kolors-1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Environment Setup

For production deployment:

1. Set appropriate database configuration
2. Configure logging levels
3. Set up reverse proxy (nginx/Apache)
4. Configure SSL certificates
5. Set up monitoring and health checks

## 📈 Performance

### Optimization Features

- **Database Indexes**: Optimized indexes for all search queries
- **Query Optimization**: Efficient JPA queries with proper joins
- **Caching**: Strategic caching for frequently accessed data
- **Lazy Loading**: Optimized entity relationships
- **Batch Processing**: Efficient bulk operations

### Performance Metrics

- **Search Response Time**: < 100ms for typical queries
- **Page Load Time**: < 2s on mobile networks
- **Database Query Time**: < 50ms for indexed searches
- **Memory Usage**: Optimized for low memory footprint

## 🧪 Testing

### Test Coverage

- **Unit Tests**: Service layer and utility functions
- **Integration Tests**: Database operations and API endpoints
- **Performance Tests**: Load testing and optimization validation
- **UI Tests**: Web interface and responsive design
- **End-to-End Tests**: Complete user workflows

### Running Specific Tests

```bash
# Performance tests
./gradlew test --tests "*Performance*"

# Integration tests
./gradlew test --tests "*Integration*"

# UI validation tests
./gradlew test --tests "*UI*"
```

## 🔄 Migration Status

The application has completed migration from the legacy KolorKombination system:

- ✅ **Migration Completed**: All legacy data successfully migrated
- ✅ **Legacy Code Removed**: All legacy classes and tables removed
- ✅ **Data Validated**: Complete data integrity validation
- ✅ **System Optimized**: Performance improvements implemented

Access the migration status at: http://localhost:8080/admin/migration/status

## 🤝 Contributing

### Development Workflow

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass (`./gradlew test`)
6. Commit your changes (`git commit -m 'feat: add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

### Code Standards

- Follow Java naming conventions
- Write comprehensive tests
- Update documentation for new features
- Use conventional commit messages
- Ensure responsive design for UI changes

## 📝 License

This project is licensed under the MIT License with Attribution Requirement.

**Key Points:**

- ✅ Free to use, modify, and distribute
- ✅ Commercial use allowed
- ✅ Private use allowed
- ⚠️ **Attribution Required**: Must credit "Original software by Kreaker.dev"

See the [LICENSE](LICENSE) file for complete details.

### Attribution Examples

When using or modifying this software, include attribution such as:

- Footer: "Powered by Kolors - Original software by Kreaker.dev"
- About page: "Based on Kolors by Kreaker.dev"
- Documentation: "Original development by Kreaker.dev"

## 🆘 Support

### Common Issues

**Application won't start**

- Check Java version (requires Java 17+)
- Verify port 8080 is available
- Check database permissions

**Search not working**

- Verify database indexes are created
- Check application logs for errors
- Ensure proper data format

**Mobile interface issues**

- Clear browser cache
- Check responsive CSS loading
- Verify viewport meta tag

### Getting Help

- Check the [documentation](docs/)
- Review [performance optimizations](docs/PERFORMANCE_OPTIMIZATIONS.md)
- Check [migration status](docs/MIGRATION_COMPLETION_SUMMARY.md)
- Open an issue for bugs or feature requests

## 🎯 Roadmap

### Upcoming Features

- [ ] Color palette export (PNG, SVG)
- [ ] Color harmony suggestions
- [ ] User accounts and favorites
- [ ] Advanced color theory tools
- [ ] API rate limiting
- [ ] Internationalization (i18n)

### Performance Improvements

- [ ] Redis caching layer
- [ ] Database connection pooling
- [ ] CDN integration for static assets
- [ ] Progressive Web App (PWA) features

---

**Kolors** - Making color combination management simple and beautiful. 🎨
