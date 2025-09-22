package dev.kreaker.kolors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite::memory:",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class ColorCombinationRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private ColorCombinationRepository colorCombinationRepository;

  private ColorCombination combination1;
  private ColorCombination combination2;
  private ColorCombination combination3;

  @BeforeEach
  void setUp() {
    // Create test data
    combination1 = new ColorCombination("Sunset Colors", 3);
    combination1.addColor(new ColorInCombination("FF6B35", 1));
    combination1.addColor(new ColorInCombination("F7931E", 2));
    combination1.addColor(new ColorInCombination("FFD23F", 3));

    combination2 = new ColorCombination("Ocean Blues", 2);
    combination2.addColor(new ColorInCombination("1E3A8A", 1));
    combination2.addColor(new ColorInCombination("3B82F6", 2));

    combination3 = new ColorCombination("Forest Greens", 4);
    combination3.addColor(new ColorInCombination("065F46", 1));
    combination3.addColor(new ColorInCombination("059669", 2));
    combination3.addColor(new ColorInCombination("10B981", 3));
    combination3.addColor(new ColorInCombination("6EE7B7", 4));

    // Persist test data
    entityManager.persistAndFlush(combination1);
    entityManager.persistAndFlush(combination2);
    entityManager.persistAndFlush(combination3);
  }

  @Test
  @DisplayName("Should find combinations by name containing (case insensitive)")
  void shouldFindByNameContainingIgnoreCase() {
    // When
    List<ColorCombination> results =
        colorCombinationRepository.findByNameContainingIgnoreCase("ocean");

    // Then
    assertEquals(1, results.size());
    assertEquals("Ocean Blues", results.get(0).getName());

    // Test case insensitive
    results = colorCombinationRepository.findByNameContainingIgnoreCase("SUNSET");
    assertEquals(1, results.size());
    assertEquals("Sunset Colors", results.get(0).getName());
  }

  @Test
  @DisplayName("Should find combinations by color count")
  void shouldFindByColorCount() {
    // When
    List<ColorCombination> twoColorCombinations = colorCombinationRepository.findByColorCount(2);
    List<ColorCombination> threeColorCombinations = colorCombinationRepository.findByColorCount(3);
    List<ColorCombination> fourColorCombinations = colorCombinationRepository.findByColorCount(4);

    // Then
    assertEquals(1, twoColorCombinations.size());
    assertEquals("Ocean Blues", twoColorCombinations.get(0).getName());

    assertEquals(1, threeColorCombinations.size());
    assertEquals("Sunset Colors", threeColorCombinations.get(0).getName());

    assertEquals(1, fourColorCombinations.size());
    assertEquals("Forest Greens", fourColorCombinations.get(0).getName());
  }

  @Test
  @DisplayName("Should find combinations by date range")
  void shouldFindByCreatedAtBetween() {
    // Given
    LocalDateTime start = LocalDateTime.now().minusHours(1);
    LocalDateTime end = LocalDateTime.now().plusHours(1);

    // When
    List<ColorCombination> results = colorCombinationRepository.findByCreatedAtBetween(start, end);

    // Then
    assertEquals(3, results.size());
  }

  @Test
  @DisplayName("Should find all combinations ordered by creation date desc")
  void shouldFindAllByOrderByCreatedAtDesc() {
    // When
    List<ColorCombination> results = colorCombinationRepository.findAllByOrderByCreatedAtDesc();

    // Then
    assertEquals(3, results.size());
    // Results should be ordered by creation date descending
    for (int i = 0; i < results.size() - 1; i++) {
      assertTrue(
          results.get(i).getCreatedAt().isAfter(results.get(i + 1).getCreatedAt())
              || results.get(i).getCreatedAt().isEqual(results.get(i + 1).getCreatedAt()));
    }
  }

  @Test
  @DisplayName("Should find combinations containing specific hex value")
  void shouldFindByContainingHexValue() {
    // When
    List<ColorCombination> results = colorCombinationRepository.findByContainingHexValue("FF6B35");

    // Then
    assertEquals(1, results.size());
    assertEquals("Sunset Colors", results.get(0).getName());

    // Test with non-existing hex value
    results = colorCombinationRepository.findByContainingHexValue("FFFFFF");
    assertTrue(results.isEmpty());
  }

  @Test
  @DisplayName("Should find combinations with pagination")
  void shouldFindAllWithPagination() {
    // Given
    Pageable pageable = PageRequest.of(0, 2);

    // When
    Page<ColorCombination> page = colorCombinationRepository.findAllByOrderByNameAsc(pageable);

    // Then
    assertEquals(2, page.getContent().size());
    assertEquals(3, page.getTotalElements());
    assertEquals(2, page.getTotalPages());
    assertTrue(page.hasNext());
  }

  @Test
  @DisplayName("Should find combinations by name with pagination")
  void shouldFindByNameContainingWithPagination() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<ColorCombination> page =
        colorCombinationRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(
            "Colors", pageable);

    // Then
    assertEquals(1, page.getContent().size()); // Only "Sunset Colors" contains "Colors"
    assertEquals(1, page.getTotalElements());
    assertEquals("Sunset Colors", page.getContent().get(0).getName());
  }

  @Test
  @DisplayName("Should find combinations by color count with pagination")
  void shouldFindByColorCountWithPagination() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<ColorCombination> page =
        colorCombinationRepository.findByColorCountOrderByCreatedAtDesc(3, pageable);

    // Then
    assertEquals(1, page.getContent().size());
    assertEquals("Sunset Colors", page.getContent().get(0).getName());
  }

  @Test
  @DisplayName("Should count combinations by color count")
  void shouldCountByColorCount() {
    // When & Then
    assertEquals(1, colorCombinationRepository.countByColorCount(2));
    assertEquals(1, colorCombinationRepository.countByColorCount(3));
    assertEquals(1, colorCombinationRepository.countByColorCount(4));
    assertEquals(0, colorCombinationRepository.countByColorCount(5));
  }

  @Test
  @DisplayName("Should find combinations containing all specified hex values")
  void shouldFindByContainingAllHexValues() {
    // Given
    List<String> hexValues = Arrays.asList("FF6B35", "F7931E");

    // When
    List<ColorCombination> results =
        colorCombinationRepository.findByContainingAllHexValues(hexValues, 2);

    // Then
    assertEquals(1, results.size());
    assertEquals("Sunset Colors", results.get(0).getName());

    // Test with hex values that don't exist together
    hexValues = Arrays.asList("FF6B35", "1E3A8A");
    results = colorCombinationRepository.findByContainingAllHexValues(hexValues, 2);
    assertTrue(results.isEmpty());
  }

  @Test
  @DisplayName("Should find combinations created after specific date")
  void shouldFindByCreatedAtAfter() {
    // Given
    LocalDateTime cutoffDate = LocalDateTime.now().minusMinutes(30);

    // When
    List<ColorCombination> results =
        colorCombinationRepository.findByCreatedAtAfterOrderByCreatedAtDesc(cutoffDate);

    // Then
    assertEquals(3, results.size());

    // Test with future date
    cutoffDate = LocalDateTime.now().plusHours(1);
    results = colorCombinationRepository.findByCreatedAtAfterOrderByCreatedAtDesc(cutoffDate);
    assertTrue(results.isEmpty());
  }

  @Test
  @DisplayName("Should find most recent combinations")
  void shouldFindMostRecent() {
    // Given
    Pageable pageable = PageRequest.of(0, 2);

    // When
    List<ColorCombination> results = colorCombinationRepository.findMostRecent(pageable);

    // Then
    assertEquals(2, results.size());
    // Should be ordered by creation date descending
    assertTrue(
        results.get(0).getCreatedAt().isAfter(results.get(1).getCreatedAt())
            || results.get(0).getCreatedAt().isEqual(results.get(1).getCreatedAt()));
  }

  @Test
  @DisplayName("Should check if combination exists by name (case insensitive)")
  void shouldCheckExistsByNameIgnoreCase() {
    // When & Then
    assertTrue(colorCombinationRepository.existsByNameIgnoreCase("sunset colors"));
    assertTrue(colorCombinationRepository.existsByNameIgnoreCase("OCEAN BLUES"));
    assertFalse(colorCombinationRepository.existsByNameIgnoreCase("Non Existing"));
  }

  @Test
  @DisplayName("Should find similar combinations")
  void shouldFindSimilarCombinations() {
    // When
    List<ColorCombination> results = colorCombinationRepository.findSimilarCombinations("Color", 3);

    // Then
    assertEquals(1, results.size());
    assertEquals("Sunset Colors", results.get(0).getName());

    // Test with different color count
    results = colorCombinationRepository.findSimilarCombinations("Color", 2);
    assertTrue(results.isEmpty());
  }

  @Test
  @DisplayName("Should save and retrieve combination correctly")
  void shouldSaveAndRetrieveCombination() {
    // Given
    ColorCombination newCombination = new ColorCombination("Test Combination", 2);
    newCombination.addColor(new ColorInCombination("ABCDEF", 1));
    newCombination.addColor(new ColorInCombination("123456", 2));

    // When
    ColorCombination saved = colorCombinationRepository.save(newCombination);
    Optional<ColorCombination> retrieved = colorCombinationRepository.findById(saved.getId());

    // Then
    assertTrue(retrieved.isPresent());
    assertEquals("Test Combination", retrieved.get().getName());
    assertEquals(2, retrieved.get().getColorCount());
    assertEquals(2, retrieved.get().getColors().size());
  }

  @Test
  @DisplayName("Should delete combination and cascade to colors")
  void shouldDeleteCombinationAndCascadeToColors() {
    // Given
    Long combinationId = combination1.getId();

    // When
    colorCombinationRepository.deleteById(combinationId);
    entityManager.flush();

    // Then
    Optional<ColorCombination> deleted = colorCombinationRepository.findById(combinationId);
    assertFalse(deleted.isPresent());

    // Verify total count decreased
    assertEquals(2, colorCombinationRepository.count());
  }
}
