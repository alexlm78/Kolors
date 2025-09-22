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

  /** Finds all colors of a specific combination ordered by position */
  List<ColorInCombination> findByCombinationIdOrderByPosition(Long combinationId);

  /** Finds colors with position greater than specified value for reordering */
  List<ColorInCombination> findByCombinationIdAndPositionGreaterThanOrderByPosition(
      Long combinationId, Integer position);

  /** Busca colores por valor hexadecimal específico */
  List<ColorInCombination> findByHexValue(String hexValue);

  /** Busca colores por valor hexadecimal ignorando mayúsculas/minúsculas */
  List<ColorInCombination> findByHexValueIgnoreCase(String hexValue);

  /** Finds a specific color in a combination by position */
  Optional<ColorInCombination> findByCombinationIdAndPosition(Long combinationId, Integer position);

  /** Finds colors in a specific combination */
  List<ColorInCombination> findByCombination(ColorCombination combination);

  /** Counts how many colors a specific combination has */
  long countByCombinationId(Long combinationId);

  /** Busca colores por rango de posiciones */
  List<ColorInCombination> findByPositionBetween(Integer startPosition, Integer endPosition);

  /** Verifies if a color exists in a specific position of a combination */
  boolean existsByCombinationIdAndPosition(Long combinationId, Integer position);

  /** Verifies if a specific hexadecimal value exists in a combination */
  boolean existsByCombinationIdAndHexValue(Long combinationId, String hexValue);

  /** Elimina todos los colores de una combinación específica */
  @Modifying
  void deleteByCombinationId(Long combinationId);

  /** Busca colores que coincidan con múltiples valores hexadecimales */
  List<ColorInCombination> findByHexValueIn(List<String> hexValues);

  /** Busca la posición máxima utilizada en una combinación */
  @Query(
      "SELECT MAX(cic.position) FROM ColorInCombination cic WHERE cic.combination.id = :combinationId")
  Optional<Integer> findMaxPositionByCombinationId(@Param("combinationId") Long combinationId);

  /** Busca colores duplicados por valor hexadecimal (útil para validación) */
  @Query(
      "SELECT cic FROM ColorInCombination cic "
          + "WHERE cic.hexValue IN ("
          + "  SELECT cic2.hexValue FROM ColorInCombination cic2 "
          + "  GROUP BY cic2.hexValue "
          + "  HAVING COUNT(cic2.hexValue) > 1"
          + ")")
  List<ColorInCombination> findDuplicateHexValues();

  /** Busca colores por patrón de valor hexadecimal (útil para búsquedas parciales) */
  @Query("SELECT cic FROM ColorInCombination cic " + "WHERE cic.hexValue LIKE :pattern")
  List<ColorInCombination> findByHexValuePattern(@Param("pattern") String pattern);

  /** Cuenta cuántas veces se usa un color específico en todas las combinaciones */
  @Query("SELECT COUNT(cic) FROM ColorInCombination cic WHERE cic.hexValue = :hexValue")
  long countUsageOfHexValue(@Param("hexValue") String hexValue);

  /** Busca los colores más utilizados */
  @Query(
      "SELECT cic.hexValue, COUNT(cic.hexValue) as usage_count "
          + "FROM ColorInCombination cic "
          + "GROUP BY cic.hexValue "
          + "ORDER BY COUNT(cic.hexValue) DESC")
  List<Object[]> findMostUsedColors();

  /** Busca colores en combinaciones creadas después de una fecha específica */
  @Query(
      "SELECT cic FROM ColorInCombination cic "
          + "WHERE cic.combination.createdAt > :date "
          + "ORDER BY cic.combination.createdAt DESC")
  List<ColorInCombination> findByCreatedAfter(@Param("date") java.time.LocalDateTime date);
}
