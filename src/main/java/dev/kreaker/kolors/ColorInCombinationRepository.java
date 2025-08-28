package dev.kreaker.kolors;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ColorInCombinationRepository extends JpaRepository<ColorInCombination, Long> {
    
    /**
     * Busca todos los colores de una combinación específica ordenados por posición
     */
    List<ColorInCombination> findByCombinationIdOrderByPosition(Long combinationId);
    
    /**
     * Busca colores por valor hexadecimal específico
     */
    List<ColorInCombination> findByHexValue(String hexValue);
    
    /**
     * Busca colores por valor hexadecimal ignorando mayúsculas/minúsculas
     */
    List<ColorInCombination> findByHexValueIgnoreCase(String hexValue);
    
    /**
     * Busca un color específico en una combinación por posición
     */
    Optional<ColorInCombination> findByCombinationIdAndPosition(Long combinationId, Integer position);
    
    /**
     * Busca colores en una combinación específica
     */
    List<ColorInCombination> findByCombination(ColorCombination combination);
    
    /**
     * Cuenta cuántos colores tiene una combinación específica
     */
    long countByCombinationId(Long combinationId);
    
    /**
     * Busca colores por rango de posiciones
     */
    List<ColorInCombination> findByPositionBetween(Integer startPosition, Integer endPosition);
    
    /**
     * Verifica si existe un color en una posición específica de una combinación
     */
    boolean existsByCombinationIdAndPosition(Long combinationId, Integer position);
    
    /**
     * Verifica si existe un valor hexadecimal específico en una combinación
     */
    boolean existsByCombinationIdAndHexValue(Long combinationId, String hexValue);
    
    /**
     * Elimina todos los colores de una combinación específica
     */
    @Modifying
    void deleteByCombinationId(Long combinationId);
    
    /**
     * Busca colores que coincidan con múltiples valores hexadecimales
     */
    List<ColorInCombination> findByHexValueIn(List<String> hexValues);
    
    /**
     * Busca la posición máxima utilizada en una combinación
     */
    @Query("SELECT MAX(cic.position) FROM ColorInCombination cic WHERE cic.combination.id = :combinationId")
    Optional<Integer> findMaxPositionByCombinationId(@Param("combinationId") Long combinationId);
    
    /**
     * Busca colores duplicados por valor hexadecimal (útil para validación)
     */
    @Query("SELECT cic FROM ColorInCombination cic " +
           "WHERE cic.hexValue IN (" +
           "  SELECT cic2.hexValue FROM ColorInCombination cic2 " +
           "  GROUP BY cic2.hexValue " +
           "  HAVING COUNT(cic2.hexValue) > 1" +
           ")")
    List<ColorInCombination> findDuplicateHexValues();
    
    /**
     * Busca colores por patrón de valor hexadecimal (útil para búsquedas parciales)
     */
    @Query("SELECT cic FROM ColorInCombination cic " +
           "WHERE cic.hexValue LIKE :pattern")
    List<ColorInCombination> findByHexValuePattern(@Param("pattern") String pattern);
    
    /**
     * Cuenta cuántas veces se usa un color específico en todas las combinaciones
     */
    @Query("SELECT COUNT(cic) FROM ColorInCombination cic WHERE cic.hexValue = :hexValue")
    long countUsageOfHexValue(@Param("hexValue") String hexValue);
    
    /**
     * Busca los colores más utilizados
     */
    @Query("SELECT cic.hexValue, COUNT(cic.hexValue) as usage_count " +
           "FROM ColorInCombination cic " +
           "GROUP BY cic.hexValue " +
           "ORDER BY COUNT(cic.hexValue) DESC")
    List<Object[]> findMostUsedColors();
    
    /**
     * Busca colores en combinaciones creadas después de una fecha específica
     */
    @Query("SELECT cic FROM ColorInCombination cic " +
           "WHERE cic.combination.createdAt > :date " +
           "ORDER BY cic.combination.createdAt DESC")
    List<ColorInCombination> findByCreatedAfter(@Param("date") java.time.LocalDateTime date);
}