# Documento de Diseño

## Visión General

El diseño transforma la aplicación Kolors de un sistema de colores individuales a uno que maneja combinaciones de colores múltiples de 2, 3 o 4 colores. La arquitectura mantiene la simplicidad del diseño actual basado en Spring Boot con JPA/Hibernate, pero introduce un nuevo modelo de datos que soporta relaciones uno-a-muchos entre combinaciones y colores individuales. El sistema prioriza la experiencia de usuario con interfaces responsivas, validación robusta y migración transparente de datos existentes.

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
                                               │     sqlite      │
                                               │   (local DB)    │
                                               └─────────────────┘
```

### Componentes Principales

1. **Capa de Presentación**: Thymeleaf templates responsivos con JavaScript para interactividad dinámica y validación en tiempo real
2. **Capa de Control**: Spring MVC controllers con manejo robusto de errores y validación
3. **Capa de Servicio**: Servicios de negocio para lógica de aplicación y migración de datos
4. **Capa de Persistencia**: Spring Data JPA repositories con consultas optimizadas
5. **Base de Datos**: SQLite para desarrollo y producción ligera, con configuración específica para Spring Boot
6. **Sistema de Migración**: Componentes especializados para transición de datos legacy

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
    public String index(Model model, @RequestParam(required = false) String search,
                       @RequestParam(required = false) Integer colorCount);
    
    @PostMapping("/create")
    public String createCombination(@Valid @ModelAttribute ColorCombinationForm form,
                                   BindingResult result, Model model);
    
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model);
    
    @PostMapping("/{id}/update")
    public String updateCombination(@PathVariable Long id, 
                                   @Valid @ModelAttribute ColorCombinationForm form,
                                   BindingResult result, Model model);
    
    @PostMapping("/{id}/delete")
    public String deleteCombination(@PathVariable Long id, RedirectAttributes redirectAttributes);
    
    @GetMapping("/{id}/confirm-delete")
    public String confirmDelete(@PathVariable Long id, Model model);
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
    public List<ColorCombination> searchCombinations(String searchTerm, Integer colorCount);
    public Optional<ColorCombination> findById(Long id);
    public ColorCombination updateCombination(Long id, ColorCombinationForm form);
    public void deleteCombination(Long id);
    public boolean validateHexColors(List<String> hexValues);
    public boolean validateColorCount(Integer colorCount, List<ColorForm> colors);
    public ColorCombinationForm convertToForm(ColorCombination combination);
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
    List<ColorCombination> findByNameContainingIgnoreCaseAndColorCount(String name, Integer colorCount);
    List<ColorCombination> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<ColorCombination> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT DISTINCT cc FROM ColorCombination cc JOIN cc.colors cic WHERE cic.hexValue = :hexValue")
    List<ColorCombination> findByColorHexValue(@Param("hexValue") String hexValue);
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

## Configuración de Base de Datos

### Configuración SQLite

La aplicación utilizará SQLite como base de datos principal con las siguientes configuraciones:

#### Dependencias Maven/Gradle
```gradle
implementation 'org.xerial:sqlite-jdbc:3.44.1.0'
```

#### Configuración Spring Boot (application.properties)
```properties
# SQLite Database Configuration
spring.datasource.url=jdbc:sqlite:kolors.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.datasource.username=
spring.datasource.password=

# JPA/Hibernate Configuration for SQLite
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# SQLite specific settings
spring.jpa.properties.hibernate.connection.handling_mode=delayed_acquisition_and_release_after_transaction
```

#### Configuración de Entidades para SQLite
```java
@Entity
public class ColorCombination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // SQLite no soporta TIMESTAMP con DEFAULT, usar @CreationTimestamp
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    // Resto de campos...
}
```

## Modelos de Datos

### Esquema de Base de Datos

```sql
-- SQLite Schema - Nueva tabla principal
CREATE TABLE color_combination (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    color_count INTEGER NOT NULL CHECK (color_count BETWEEN 2 AND 4)
);

-- SQLite Schema - Nueva tabla de colores en combinación
CREATE TABLE color_in_combination (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    hex_value VARCHAR(6) NOT NULL,
    position INTEGER NOT NULL CHECK (position BETWEEN 1 AND 4),
    combination_id INTEGER NOT NULL,
    FOREIGN KEY (combination_id) REFERENCES color_combination(id) ON DELETE CASCADE,
    UNIQUE (combination_id, position)
);

-- Índices para optimización
CREATE INDEX idx_combination_name ON color_combination(name);
CREATE INDEX idx_combination_created_at ON color_combination(created_at);
CREATE INDEX idx_color_hex ON color_in_combination(hex_value);

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
   - Validación personalizada para valores hexadecimales (exactamente 6 caracteres alfanuméricos)
   - Validación de consistencia entre colorCount y número de colores proporcionados
   - Validación de nombres de combinación (mínimo 3 caracteres)
   - Validación en tiempo real con JavaScript para retroalimentación inmediata

2. **Excepciones de Negocio**
   ```java
   public class ColorCombinationNotFoundException extends RuntimeException
   public class InvalidColorFormatException extends RuntimeException
   public class MigrationException extends RuntimeException
   public class InvalidColorCountException extends RuntimeException
   ```

3. **Manejo Global de Excepciones**
   ```java
   @ControllerAdvice
   public class GlobalExceptionHandler {
       @ExceptionHandler(ColorCombinationNotFoundException.class)
       public String handleNotFound(Model model, Exception e);
       
       @ExceptionHandler(ValidationException.class)
       public String handleValidation(Model model, Exception e);
       
       @ExceptionHandler(InvalidColorFormatException.class)
       public String handleInvalidColorFormat(Model model, Exception e);
       
       @ExceptionHandler(Exception.class)
       public String handleGenericError(Model model, Exception e);
   }
   ```

4. **Validación de Formularios**
   - Prevención de envío con campos incompletos
   - Resaltado visual de campos con errores
   - Mensajes de error específicos y contextuales
   - Preservación de datos válidos durante corrección de errores

### Mensajes de Error Localizados

```properties
# messages.properties
error.combination.notfound=Combinación de colores no encontrada
error.color.invalid.hex=Formato de color hexadecimal inválido: debe ser exactamente 6 caracteres (0-9, A-F)
error.combination.name.required=El nombre de la combinación es obligatorio
error.combination.name.minlength=El nombre debe tener al menos 3 caracteres
error.combination.colorcount.required=Debe especificar el número de colores (2, 3 o 4)
error.combination.colorcount.invalid=El número de colores debe ser 2, 3 o 4
error.combination.colors.mismatch=El número de colores proporcionados no coincide con la cantidad seleccionada
error.migration.failed=Error durante la migración de datos
error.server.generic=Ha ocurrido un error inesperado. Por favor, inténtelo de nuevo
error.combination.delete.failed=No se pudo eliminar la combinación de colores
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

## Funcionalidad de Búsqueda y Filtrado

### Capacidades de Búsqueda

1. **Búsqueda por Nombre**
   - Búsqueda case-insensitive por nombre de combinación
   - Búsqueda parcial con coincidencias substring
   - Resultados ordenados por relevancia y fecha de creación

2. **Filtrado por Características**
   - Filtro por número de colores (2, 3, o 4)
   - Búsqueda por valores hexadecimales específicos
   - Combinación de múltiples criterios de filtrado

3. **Interfaz de Búsqueda**
   ```java
   @GetMapping("/search")
   public String searchCombinations(@RequestParam(required = false) String name,
                                   @RequestParam(required = false) Integer colorCount,
                                   @RequestParam(required = false) String hexValue,
                                   Model model);
   ```

### Implementación de Búsqueda

- Uso de consultas JPA optimizadas con índices apropiados
- Paginación para manejar grandes volúmenes de resultados
- Búsqueda en tiempo real con JavaScript para mejor UX
- Preservación de criterios de búsqueda durante navegación

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
   - Carga dinámica de campos de color según selección (2, 3 o 4 colores)
   - Validación en tiempo real de valores hexadecimales
   - Preview en vivo de combinaciones de colores
   - Confirmación interactiva para eliminación de combinaciones
   - Búsqueda y filtrado dinámico sin recarga de página

2. **CSS Responsivo**
   - Uso de CSS Grid/Flexbox para layouts adaptativos
   - Media queries para optimización móvil y desktop
   - Campos de formulario optimizados para interacción táctil
   - Visualización adaptativa de combinaciones según tamaño de pantalla
   - Ajuste dinámico para cambios de orientación

3. **Experiencia de Usuario**
   - Vista previa visual clara de cada combinación con valores hexadecimales
   - Organización clara en tabla/lista para múltiples combinaciones
   - Formularios pre-llenados para edición
   - Retroalimentación visual inmediata para validaciones

4. **Diseño Responsivo**
   - Adaptación automática a diferentes tamaños de pantalla
   - Optimización específica para dispositivos móviles y tablets
   - Campos de formulario fácilmente seleccionables en pantallas táctiles
   - Visualización clara de combinaciones en pantallas pequeñas
   - Navegación intuitiva en todos los dispositivos

## Migración y Compatibilidad

### Plan de Migración

1. **Fase 1: Preparación**
   - Crear nuevas tablas sin afectar las existentes
   - Implementar servicios de migración con validación robusta
   - Crear endpoint de migración con interfaz administrativa
   - Implementar detección automática de datos legacy

2. **Fase 2: Migración de Datos**
   - Convertir automáticamente colores individuales en combinaciones de un color
   - Validar integridad de datos migrados con verificaciones exhaustivas
   - Crear backup automático de datos originales antes de migración
   - Mantener datos originales intactos durante el proceso

3. **Fase 3: Transición**
   - Activar nueva funcionalidad manteniendo compatibilidad
   - Proporcionar acceso continuo a datos migrados en nueva interfaz
   - Monitorear rendimiento y errores con logging detallado
   - Implementar rollback automático en caso de fallas

4. **Fase 4: Validación y Limpieza**
   - Verificar que todos los datos legacy sean accesibles en el nuevo sistema
   - Ejecutar pruebas de integridad post-migración
   - Eliminar código y tablas legacy solo después de confirmación de éxito
   - Documentar proceso completo para futuras referencias

**Criterios de Éxito de Migración:**
- Todos los colores individuales existentes convertidos exitosamente
- Datos mostrados correctamente en nueva interfaz
- Cero pérdida de datos durante el proceso
- Funcionalidad completa disponible post-migración