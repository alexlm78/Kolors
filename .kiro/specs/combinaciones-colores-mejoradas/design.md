# Documento de Diseño

## Visión General

El diseño transforma la aplicación Kolors de un sistema de colores individuales a uno que maneja combinaciones de colores múltiples. La arquitectura mantiene la simplicidad del diseño actual basado en Spring Boot con JPA/Hibernate, pero introduce un nuevo modelo de datos que soporta relaciones uno-a-muchos entre combinaciones y colores individuales.

## Arquitectura

### Arquitectura de Alto Nivel

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Thymeleaf     │    │   Spring MVC    │    │   Spring Data   │
│   Templates     │◄───┤   Controllers   │◄───┤   JPA           │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                                               ┌─────────────────┐
                                               │   H2 Database   │
                                               │   (In-Memory)   │
                                               └─────────────────┘
```

### Componentes Principales

1. **Capa de Presentación**: Thymeleaf templates con JavaScript para interactividad dinámica
2. **Capa de Control**: Spring MVC controllers para manejar requests HTTP
3. **Capa de Servicio**: Servicios de negocio para lógica de aplicación
4. **Capa de Persistencia**: Spring Data JPA repositories
5. **Base de Datos**: H2 in-memory para desarrollo, preparada para migración a producción

## Componentes e Interfaces

### Modelo de Datos Rediseñado

#### ColorCombination (Nueva Entidad Principal)
```java
@Entity
public class ColorCombination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "combination", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ColorInCombination> colors;
    
    @Column(nullable = false)
    private Integer colorCount; // 2, 3, o 4
}
```

#### ColorInCombination (Nueva Entidad de Relación)
```java
@Entity
public class ColorInCombination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 6, pattern = "^[0-9A-Fa-f]{6}$")
    private String hexValue;
    
    @Column(nullable = false)
    private Integer position; // 1, 2, 3, o 4
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combination_id", nullable = false)
    private ColorCombination combination;
}
```

#### Migración de KolorKombination Existente
- La entidad actual se mantendrá temporalmente para migración
- Se creará un servicio de migración que convierta colores individuales en combinaciones de un solo color
- Después de la migración exitosa, la tabla antigua puede ser eliminada

### Controladores

#### ColorCombinationController (Nuevo)
```java
@Controller
@RequestMapping("/combinations")
public class ColorCombinationController {
    
    @GetMapping("/")
    public String index(Model model);
    
    @PostMapping("/create")
    public String createCombination(@Valid @ModelAttribute ColorCombinationForm form);
    
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model);
    
    @PostMapping("/{id}/update")
    public String updateCombination(@PathVariable Long id, @Valid @ModelAttribute ColorCombinationForm form);
    
    @PostMapping("/{id}/delete")
    public String deleteCombination(@PathVariable Long id);
}
```

#### MigrationController (Temporal)
```java
@Controller
@RequestMapping("/migration")
public class MigrationController {
    
    @PostMapping("/migrate-legacy-colors")
    public String migrateLegacyColors();
    
    @GetMapping("/status")
    public String migrationStatus(Model model);
}
```

### Servicios

#### ColorCombinationService
```java
@Service
public class ColorCombinationService {
    
    public ColorCombination createCombination(ColorCombinationForm form);
    public List<ColorCombination> findAllCombinations();
    public Optional<ColorCombination> findById(Long id);
    public ColorCombination updateCombination(Long id, ColorCombinationForm form);
    public void deleteCombination(Long id);
    public boolean validateHexColors(List<String> hexValues);
}
```

#### MigrationService
```java
@Service
public class MigrationService {
    
    public MigrationResult migrateLegacyColors();
    public boolean isLegacyDataPresent();
    public MigrationStatus getMigrationStatus();
}
```

### Repositorios

#### ColorCombinationRepository
```java
@Repository
public interface ColorCombinationRepository extends JpaRepository<ColorCombination, Long> {
    List<ColorCombination> findByNameContainingIgnoreCase(String name);
    List<ColorCombination> findByColorCount(Integer colorCount);
    List<ColorCombination> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
```

#### ColorInCombinationRepository
```java
@Repository
public interface ColorInCombinationRepository extends JpaRepository<ColorInCombination, Long> {
    List<ColorInCombination> findByCombinationIdOrderByPosition(Long combinationId);
    List<ColorInCombination> findByHexValue(String hexValue);
}
```

## Modelos de Datos

### Esquema de Base de Datos

```sql
-- Nueva tabla principal
CREATE TABLE color_combination (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    color_count INTEGER NOT NULL CHECK (color_count BETWEEN 2 AND 4)
);

-- Nueva tabla de colores en combinación
CREATE TABLE color_in_combination (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hex_value VARCHAR(6) NOT NULL,
    position INTEGER NOT NULL CHECK (position BETWEEN 1 AND 4),
    combination_id BIGINT NOT NULL,
    FOREIGN KEY (combination_id) REFERENCES color_combination(id) ON DELETE CASCADE,
    UNIQUE KEY unique_position_per_combination (combination_id, position)
);

-- Tabla legacy (se mantendrá temporalmente)
-- kolor_kombination (existente)
```

### DTOs y Forms

#### ColorCombinationForm
```java
public class ColorCombinationForm {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String name;
    
    @NotNull(message = "Debe especificar el número de colores")
    @Min(value = 2, message = "Mínimo 2 colores")
    @Max(value = 4, message = "Máximo 4 colores")
    private Integer colorCount;
    
    @Valid
    private List<ColorForm> colors;
}
```

#### ColorForm
```java
public class ColorForm {
    @NotBlank(message = "El valor hexadecimal es obligatorio")
    @Pattern(regexp = "^[0-9A-Fa-f]{6}$", message = "Formato hexadecimal inválido")
    private String hexValue;
    
    @NotNull
    @Min(1) @Max(4)
    private Integer position;
}
```

## Manejo de Errores

### Estrategia de Manejo de Errores

1. **Validación de Entrada**
   - Bean Validation con anotaciones JSR-303
   - Validación personalizada para valores hexadecimales
   - Validación de consistencia entre colorCount y número de colores proporcionados

2. **Excepciones de Negocio**
   ```java
   public class ColorCombinationNotFoundException extends RuntimeException
   public class InvalidColorFormatException extends RuntimeException
   public class MigrationException extends RuntimeException
   ```

3. **Manejo Global de Excepciones**
   ```java
   @ControllerAdvice
   public class GlobalExceptionHandler {
       @ExceptionHandler(ColorCombinationNotFoundException.class)
       public String handleNotFound(Model model, Exception e);
       
       @ExceptionHandler(ValidationException.class)
       public String handleValidation(Model model, Exception e);
   }
   ```

### Mensajes de Error Localizados

```properties
# messages.properties
error.combination.notfound=Combinación de colores no encontrada
error.color.invalid.hex=Formato de color hexadecimal inválido: debe ser 6 caracteres (0-9, A-F)
error.combination.name.required=El nombre de la combinación es obligatorio
error.migration.failed=Error durante la migración de datos
```

## Estrategia de Testing

### Niveles de Testing

1. **Tests Unitarios**
   - Servicios de negocio
   - Validadores personalizados
   - Utilidades de conversión

2. **Tests de Integración**
   - Repositorios JPA
   - Controladores con MockMvc
   - Migración de datos

3. **Tests End-to-End**
   - Flujos completos de usuario
   - Validación de UI con Selenium (opcional)

### Estructura de Tests

```java
// Ejemplo de test de servicio
@ExtendWith(MockitoExtension.class)
class ColorCombinationServiceTest {
    
    @Test
    void shouldCreateValidCombination() {
        // Given
        ColorCombinationForm form = createValidForm();
        
        // When
        ColorCombination result = service.createCombination(form);
        
        // Then
        assertThat(result.getName()).isEqualTo(form.getName());
        assertThat(result.getColors()).hasSize(form.getColorCount());
    }
}

// Ejemplo de test de controlador
@WebMvcTest(ColorCombinationController.class)
class ColorCombinationControllerTest {
    
    @Test
    void shouldDisplayCombinationsPage() throws Exception {
        mockMvc.perform(get("/combinations/"))
               .andExpect(status().isOk())
               .andExpect(view().name("combinations/index"))
               .andExpect(model().attributeExists("combinations"));
    }
}
```

### Datos de Test

```java
@TestConfiguration
public class TestDataConfiguration {
    
    @Bean
    @Primary
    public ColorCombinationService testColorCombinationService() {
        return new ColorCombinationService() {
            // Implementación con datos de prueba
        };
    }
}
```

## Consideraciones de Rendimiento

### Optimizaciones de Base de Datos

1. **Índices Estratégicos**
   ```sql
   CREATE INDEX idx_combination_name ON color_combination(name);
   CREATE INDEX idx_combination_created_at ON color_combination(created_at);
   CREATE INDEX idx_color_hex ON color_in_combination(hex_value);
   ```

2. **Fetch Strategies**
   - LAZY loading para relaciones OneToMany
   - Uso de @EntityGraph para consultas específicas que requieren eager loading

3. **Paginación**
   - Implementar Pageable en repositorios para listas grandes
   - Límites de resultados en consultas de búsqueda

### Optimizaciones de Frontend

1. **JavaScript Dinámico**
   - Carga dinámica de campos de color según selección
   - Validación en tiempo real de valores hexadecimales
   - Preview en vivo de combinaciones de colores

2. **CSS Eficiente**
   - Uso de CSS Grid/Flexbox para layouts responsivos
   - Optimización de renderizado de colores
   - Lazy loading de imágenes si se implementan previews complejos

## Migración y Compatibilidad

### Plan de Migración

1. **Fase 1: Preparación**
   - Crear nuevas tablas sin afectar las existentes
   - Implementar servicios de migración
   - Crear endpoint de migración con interfaz administrativa

2. **Fase 2: Migración de Datos**
   - Convertir colores individuales en combinaciones de un color
   - Validar integridad de datos migrados
   - Crear backup de datos originales

3. **Fase 3: Transición**
   - Activar nueva funcionalidad
   - Mantener acceso de solo lectura a datos legacy
   - Monitorear rendimiento y errores

4. **Fase 4: Limpieza**
   - Eliminar código y tablas legacy después de período de gracia
   - Optimizar esquema final
   - Documentar cambios para futuros desarrolladores