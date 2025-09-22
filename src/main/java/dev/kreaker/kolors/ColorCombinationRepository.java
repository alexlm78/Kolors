package dev.kreaker.kolors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ColorCombinationRepository extends JpaRepository<ColorCombination, Long> {

  /** Searches combinations by name (case insensitive) */
  @EntityGraph(attributePaths = {"colors"})
  List<ColorCombination> findByNameContainingIgnoreCase(String name);

  /** Searches combinations by exact number of colors */
  @EntityGraph(attributePaths = {"colors"})
  List<ColorCombination> findByColorCount(Integer colorCount);

  /** Searches combinations created within a date range */
  List<ColorCombination> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

  /** Searches combinations ordered by creation date (most recent first) */
  @EntityGraph(attributePaths = {"colors"})
  List<ColorCombination> findAllByOrderByCreatedAtDesc();

  /** Searches combinations containing a specific color (by hex value) */
  @EntityGraph(attributePaths = {"colors"})
  @Query(
      "SELECT DISTINCT cc FROM ColorCombination cc "
          + "JOIN cc.colors cic "
          + "WHERE cic.hexValue = :hexValue")
  List<ColorCombination> findByContainingHexValue(@Param("hexValue") String hexValue);

  /** Searches combinations with pagination and ordering by name */
  Page<ColorCombination> findAllByOrderByNameAsc(Pageable pageable);

  /** Searches combinations by name with pagination */
  Page<ColorCombination> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(
      String name, Pageable pageable);

  /** Searches combinations by number of colors with pagination */
  Page<ColorCombination> findByColorCountOrderByCreatedAtDesc(
      Integer colorCount, Pageable pageable);

  /** Counts combinations by number of colors */
  long countByColorCount(Integer colorCount);

  /** Searches combinations containing all specified colors */
  @Query(
      "SELECT cc FROM ColorCombination cc "
          + "WHERE cc.id IN ("
          + "  SELECT cic.combination.id FROM ColorInCombination cic "
          + "  WHERE cic.hexValue IN :hexValues "
          + "  GROUP BY cic.combination.id "
          + "  HAVING COUNT(DISTINCT cic.hexValue) = :colorCount"
          + ")")
  List<ColorCombination> findByContainingAllHexValues(
      @Param("hexValues") List<String> hexValues, @Param("colorCount") long colorCount);

  /** Searches combinations created after a specific date */
  List<ColorCombination> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);

  /**
   * Searches the most popular combinations (for now only the most recent) In the future a
   * popularity or usage field could be added
   */
  @Query("SELECT cc FROM ColorCombination cc ORDER BY cc.createdAt DESC")
  List<ColorCombination> findMostRecent(Pageable pageable);

  /** Checks if a combination with the same name exists (case insensitive) */
  boolean existsByNameIgnoreCase(String name);

  /** Searches similar combinations by name and number of colors */
  @EntityGraph(attributePaths = {"colors"})
  @Query(
      "SELECT cc FROM ColorCombination cc "
          + "WHERE LOWER(cc.name) LIKE LOWER(CONCAT('%', :namePattern, '%')) "
          + "AND cc.colorCount = :colorCount "
          + "ORDER BY cc.createdAt DESC")
  List<ColorCombination> findSimilarCombinations(
      @Param("namePattern") String namePattern, @Param("colorCount") Integer colorCount);

  /** Searches combinations by color count range */
  @EntityGraph(attributePaths = {"colors"})
  List<ColorCombination> findByColorCountBetweenOrderByCreatedAtDesc(
      Integer minColors, Integer maxColors);

  /** Searches combinations by name and color count range */
  @EntityGraph(attributePaths = {"colors"})
  @Query(
      "SELECT cc FROM ColorCombination cc "
          + "WHERE LOWER(cc.name) LIKE LOWER(CONCAT('%', :namePattern, '%')) "
          + "AND cc.colorCount BETWEEN :minColors AND :maxColors "
          + "ORDER BY cc.createdAt DESC")
  List<ColorCombination> findByNameAndColorCountRange(
      @Param("namePattern") String namePattern,
      @Param("minColors") Integer minColors,
      @Param("maxColors") Integer maxColors);

  /** Searches combinations by name with pagination */
  Page<ColorCombination> findByNameContainingIgnoreCaseOrderByNameAsc(
      String name, Pageable pageable);

  /** Searches combinations by color count range with pagination */
  Page<ColorCombination> findByColorCountBetweenOrderByCreatedAtDesc(
      Integer minColors, Integer maxColors, Pageable pageable);

  /** Complex search with multiple criteria and pagination */
  @Query(
      "SELECT cc FROM ColorCombination cc "
          + "WHERE (:name IS NULL OR LOWER(cc.name) LIKE LOWER(CONCAT('%', :name, '%'))) "
          + "AND (:minColors IS NULL OR cc.colorCount >= :minColors) "
          + "AND (:maxColors IS NULL OR cc.colorCount <= :maxColors) "
          + "ORDER BY cc.createdAt DESC")
  Page<ColorCombination> findWithFilters(
      @Param("name") String name,
      @Param("minColors") Integer minColors,
      @Param("maxColors") Integer maxColors,
      Pageable pageable);

  /** Search combinations containing specific hex value with pagination */
  @Query(
      "SELECT DISTINCT cc FROM ColorCombination cc "
          + "JOIN cc.colors cic "
          + "WHERE cic.hexValue = :hexValue "
          + "ORDER BY cc.createdAt DESC")
  Page<ColorCombination> findByContainingHexValueWithPagination(
      @Param("hexValue") String hexValue, Pageable pageable);

  /** Find by ID with optimized loading of colors */
  @EntityGraph(attributePaths = {"colors"})
  @Query("SELECT cc FROM ColorCombination cc WHERE cc.id = :id")
  Optional<ColorCombination> findByIdWithColors(@Param("id") Long id);
}
