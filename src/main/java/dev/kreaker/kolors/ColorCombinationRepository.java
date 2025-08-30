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
     * Searches combinations by name (case insensitive)
     */
    List<ColorCombination> findByNameContainingIgnoreCase(String name);
    
    /**
     * Searches combinations by exact number of colors
     */
    List<ColorCombination> findByColorCount(Integer colorCount);
    
    /**
     * Searches combinations created within a date range
     */
    List<ColorCombination> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Searches combinations ordered by creation date (most recent first)
     */
    List<ColorCombination> findAllByOrderByCreatedAtDesc();
    
    /**
     * Searches combinations containing a specific color (by hex value)
     */
    @Query("SELECT DISTINCT cc FROM ColorCombination cc " +
           "JOIN cc.colors cic " +
           "WHERE cic.hexValue = :hexValue")
    List<ColorCombination> findByContainingHexValue(@Param("hexValue") String hexValue);
    
    /**
     * Searches combinations with pagination and ordering by name
     */
    Page<ColorCombination> findAllByOrderByNameAsc(Pageable pageable);
    
    /**
     * Searches combinations by name with pagination
     */
    Page<ColorCombination> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name, Pageable pageable);
    
    /**
     * Searches combinations by number of colors with pagination
     */
    Page<ColorCombination> findByColorCountOrderByCreatedAtDesc(Integer colorCount, Pageable pageable);
    
    /**
     * Counts combinations by number of colors
     */
    long countByColorCount(Integer colorCount);
    
    /**
     * Searches combinations containing all specified colors
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
     * Searches combinations created after a specific date
     */
    List<ColorCombination> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);
    
    /**
     * Searches the most popular combinations (for now only the most recent)
     * In the future a popularity or usage field could be added
     */
    @Query("SELECT cc FROM ColorCombination cc ORDER BY cc.createdAt DESC")
    List<ColorCombination> findMostRecent(Pageable pageable);
    
    /**
     * Checks if a combination with the same name exists (case insensitive)
     */
    boolean existsByNameIgnoreCase(String name);
    
    /**
     * Searches similar combinations by name and number of colors
     */
    @Query("SELECT cc FROM ColorCombination cc " +
           "WHERE LOWER(cc.name) LIKE LOWER(CONCAT('%', :namePattern, '%')) " +
           "AND cc.colorCount = :colorCount " +
           "ORDER BY cc.createdAt DESC")
    List<ColorCombination> findSimilarCombinations(@Param("namePattern") String namePattern, 
                                                   @Param("colorCount") Integer colorCount);
}