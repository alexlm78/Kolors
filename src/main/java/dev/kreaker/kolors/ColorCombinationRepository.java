package dev.kreaker.kolors;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ColorCombinationRepository extends JpaRepository<ColorCombination, Long> {
    
    /**
     * Busca combinaciones por nombre (case insensitive)
     */
    List<ColorCombination> findByNameContainingIgnoreCase(String name);
    
    /**
     * Busca combinaciones por número exacto de colores
     */
    List<ColorCombination> findByColorCount(Integer colorCount);
    
    /**
     * Busca combinaciones creadas en un rango de fechas
     */
    List<ColorCombination> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Busca combinaciones ordenadas por fecha de creación (más recientes primero)
     */
    List<ColorCombination> findAllByOrderByCreatedAtDesc();
    
    /**
     * Busca combinaciones que contengan un color específico (por valor hex)
     */
    @Query("SELECT DISTINCT cc FROM ColorCombination cc " +
           "JOIN cc.colors cic " +
           "WHERE cic.hexValue = :hexValue")
    List<ColorCombination> findByContainingHexValue(@Param("hexValue") String hexValue);
    
    /**
     * Busca combinaciones con paginación y ordenamiento por nombre
     */
    Page<ColorCombination> findAllByOrderByNameAsc(Pageable pageable);
    
    /**
     * Busca combinaciones por nombre con paginación
     */
    Page<ColorCombination> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name, Pageable pageable);
    
    /**
     * Busca combinaciones por número de colores con paginación
     */
    Page<ColorCombination> findByColorCountOrderByCreatedAtDesc(Integer colorCount, Pageable pageable);
    
    /**
     * Cuenta combinaciones por número de colores
     */
    long countByColorCount(Integer colorCount);
    
    /**
     * Busca combinaciones que contengan todos los colores especificados
     */
    @Query("SELECT cc FROM ColorCombination cc " +
           "WHERE cc.id IN (" +
           "  SELECT cic.combination.id FROM ColorInCombination cic " +
           "  WHERE cic.hexValue IN :hexValues " +
           "  GROUP BY cic.combination.id " +
           "  HAVING COUNT(DISTINCT cic.hexValue) = :colorCount" +
           ")")
    List<ColorCombination> findByContainingAllHexValues(@Param("hexValues") List<String> hexValues, 
                                                        @Param("colorCount") long colorCount);
    
    /**
     * Busca combinaciones creadas después de una fecha específica
     */
    List<ColorCombination> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);
    
    /**
     * Busca las combinaciones más populares (por ahora solo las más recientes)
     * En el futuro se podría agregar un campo de popularidad o uso
     */
    @Query("SELECT cc FROM ColorCombination cc ORDER BY cc.createdAt DESC")
    List<ColorCombination> findMostRecent(Pageable pageable);
    
    /**
     * Verifica si existe una combinación con el mismo nombre (case insensitive)
     */
    boolean existsByNameIgnoreCase(String name);
    
    /**
     * Busca combinaciones similares por nombre y número de colores
     */
    @Query("SELECT cc FROM ColorCombination cc " +
           "WHERE LOWER(cc.name) LIKE LOWER(CONCAT('%', :namePattern, '%')) " +
           "AND cc.colorCount = :colorCount " +
           "ORDER BY cc.createdAt DESC")
    List<ColorCombination> findSimilarCombinations(@Param("namePattern") String namePattern, 
                                                   @Param("colorCount") Integer colorCount);
}