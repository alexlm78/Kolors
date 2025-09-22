package dev.kreaker.kolors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class ColorInCombinationRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private ColorInCombinationRepository colorInCombinationRepository;

  private ColorCombination combination1;
  private ColorCombination combination2;
  private ColorInCombination color1;
  private ColorInCombination color2;
  private ColorInCombination color3;
  private ColorInCombination color4;

  @BeforeEach
  void setUp() {
    // Create test combinations
    combination1 = new ColorCombination("Test Combination 1", 3);
    combination2 = new ColorCombination("Test Combination 2", 2);

    // Create test colors
    color1 = new ColorInCombination("FF0000", 1);
    color2 = new ColorInCombination("00FF00", 2);
    color3 = new ColorInCombination("0000FF", 3);
    color4 = new ColorInCombination("FFFF00", 1);

    // Add colors to combinations
    combination1.addColor(color1);
    combination1.addColor(color2);
    combination1.addColor(color3);
    combination2.addColor(color4);

    // Persist test data
    entityManager.persistAndFlush(combination1);
    entityManager.persistAndFlush(combination2);
  }

  @Test
  @DisplayName("Should find colors by combination ID ordered by position")
  void shouldFindByCombinationIdOrderByPosition() {
    // When
    List<ColorInCombination> colors =
        colorInCombinationRepository.findByCombinationIdOrderByPosition(combination1.getId());

    // Then
    assertEquals(3, colors.size());
    assertEquals(1, colors.get(0).getPosition());
    assertEquals(2, colors.get(1).getPosition());
    assertEquals(3, colors.get(2).getPosition());
    assertEquals("FF0000", colors.get(0).getHexValue());
    assertEquals("00FF00", colors.get(1).getHexValue());
    assertEquals("0000FF", colors.get(2).getHexValue());
  }

  @Test
  @DisplayName("Should find colors by hex value")
  void shouldFindByHexValue() {
    // When
    List<ColorInCombination> colors = colorInCombinationRepository.findByHexValue("FF0000");

    // Then
    assertEquals(1, colors.size());
    assertEquals("FF0000", colors.get(0).getHexValue());
    assertEquals(1, colors.get(0).getPosition());

    // Test with non-existing hex value
    colors = colorInCombinationRepository.findByHexValue("FFFFFF");
    assertTrue(colors.isEmpty());
  }

  @Test
  @DisplayName("Should find colors by hex value ignoring case")
  void shouldFindByHexValueIgnoreCase() {
    // When
    List<ColorInCombination> colors =
        colorInCombinationRepository.findByHexValueIgnoreCase("ff0000");

    // Then
    assertEquals(1, colors.size());
    assertEquals("FF0000", colors.get(0).getHexValue());

    // Test with mixed case
    colors = colorInCombinationRepository.findByHexValueIgnoreCase("Ff0000");
    assertEquals(1, colors.size());
  }

  @Test
  @DisplayName("Should find color by combination ID and position")
  void shouldFindByCombinationIdAndPosition() {
    // When
    Optional<ColorInCombination> color =
        colorInCombinationRepository.findByCombinationIdAndPosition(combination1.getId(), 2);

    // Then
    assertTrue(color.isPresent());
    assertEquals("00FF00", color.get().getHexValue());
    assertEquals(2, color.get().getPosition());

    // Test with non-existing position
    color = colorInCombinationRepository.findByCombinationIdAndPosition(combination1.getId(), 5);
    assertFalse(color.isPresent());
  }

  @Test
  @DisplayName("Should find colors by combination")
  void shouldFindByCombination() {
    // When
    List<ColorInCombination> colors = colorInCombinationRepository.findByCombination(combination1);

    // Then
    assertEquals(3, colors.size());
    assertTrue(colors.stream().allMatch(c -> c.getCombination().equals(combination1)));

    // Test with combination2
    colors = colorInCombinationRepository.findByCombination(combination2);
    assertEquals(1, colors.size());
    assertEquals("FFFF00", colors.get(0).getHexValue());
  }

  @Test
  @DisplayName("Should count colors by combination ID")
  void shouldCountByCombinationId() {
    // When & Then
    assertEquals(3, colorInCombinationRepository.countByCombinationId(combination1.getId()));
    assertEquals(1, colorInCombinationRepository.countByCombinationId(combination2.getId()));
    assertEquals(0, colorInCombinationRepository.countByCombinationId(999L));
  }

  @Test
  @DisplayName("Should find colors by position range")
  void shouldFindByPositionBetween() {
    // When
    List<ColorInCombination> colors = colorInCombinationRepository.findByPositionBetween(1, 2);

    // Then
    assertEquals(3, colors.size()); // color1 (pos 1), color2 (pos 2), color4 (pos 1)
    assertTrue(colors.stream().allMatch(c -> c.getPosition() >= 1 && c.getPosition() <= 2));
  }

  @Test
  @DisplayName("Should check if color exists by combination ID and position")
  void shouldCheckExistsByCombinationIdAndPosition() {
    // When & Then
    assertTrue(
        colorInCombinationRepository.existsByCombinationIdAndPosition(combination1.getId(), 1));
    assertTrue(
        colorInCombinationRepository.existsByCombinationIdAndPosition(combination1.getId(), 2));
    assertTrue(
        colorInCombinationRepository.existsByCombinationIdAndPosition(combination1.getId(), 3));
    assertFalse(
        colorInCombinationRepository.existsByCombinationIdAndPosition(combination1.getId(), 4));
  }

  @Test
  @DisplayName("Should check if color exists by combination ID and hex value")
  void shouldCheckExistsByCombinationIdAndHexValue() {
    // When & Then
    assertTrue(
        colorInCombinationRepository.existsByCombinationIdAndHexValue(
            combination1.getId(), "FF0000"));
    assertTrue(
        colorInCombinationRepository.existsByCombinationIdAndHexValue(
            combination1.getId(), "00FF00"));
    assertFalse(
        colorInCombinationRepository.existsByCombinationIdAndHexValue(
            combination1.getId(), "FFFFFF"));
    assertFalse(
        colorInCombinationRepository.existsByCombinationIdAndHexValue(
            combination2.getId(), "FF0000"));
  }

  @Test
  @DisplayName("Should find colors by multiple hex values")
  void shouldFindByHexValueIn() {
    // Given
    List<String> hexValues = Arrays.asList("FF0000", "00FF00", "FFFFFF");

    // When
    List<ColorInCombination> colors = colorInCombinationRepository.findByHexValueIn(hexValues);

    // Then
    assertEquals(2, colors.size()); // Only FF0000 and 00FF00 exist
    assertTrue(colors.stream().anyMatch(c -> c.getHexValue().equals("FF0000")));
    assertTrue(colors.stream().anyMatch(c -> c.getHexValue().equals("00FF00")));
  }

  @Test
  @DisplayName("Should find max position by combination ID")
  void shouldFindMaxPositionByCombinationId() {
    // When
    Optional<Integer> maxPosition1 =
        colorInCombinationRepository.findMaxPositionByCombinationId(combination1.getId());
    Optional<Integer> maxPosition2 =
        colorInCombinationRepository.findMaxPositionByCombinationId(combination2.getId());

    // Then
    assertTrue(maxPosition1.isPresent());
    assertEquals(3, maxPosition1.get());

    assertTrue(maxPosition2.isPresent());
    assertEquals(1, maxPosition2.get());

    // Test with non-existing combination
    Optional<Integer> maxPositionNonExisting =
        colorInCombinationRepository.findMaxPositionByCombinationId(999L);
    assertFalse(maxPositionNonExisting.isPresent());
  }

  @Test
  @DisplayName("Should find colors by hex value pattern")
  void shouldFindByHexValuePattern() {
    // When
    List<ColorInCombination> colors = colorInCombinationRepository.findByHexValuePattern("FF%");

    // Then
    assertEquals(2, colors.size()); // FF0000 and FFFF00
    assertTrue(colors.stream().allMatch(c -> c.getHexValue().startsWith("FF")));

    // Test with different pattern - looking for colors containing "00"
    colors = colorInCombinationRepository.findByHexValuePattern("%00%");
    assertEquals(4, colors.size()); // All colors contain "00": FF0000, 00FF00, 0000FF, FFFF00
    assertTrue(colors.stream().anyMatch(c -> c.getHexValue().equals("FF0000")));
    assertTrue(colors.stream().anyMatch(c -> c.getHexValue().equals("00FF00")));
    assertTrue(colors.stream().anyMatch(c -> c.getHexValue().equals("0000FF")));
    assertTrue(colors.stream().anyMatch(c -> c.getHexValue().equals("FFFF00")));
  }

  @Test
  @DisplayName("Should count usage of hex value")
  void shouldCountUsageOfHexValue() {
    // When & Then
    assertEquals(1, colorInCombinationRepository.countUsageOfHexValue("FF0000"));
    assertEquals(1, colorInCombinationRepository.countUsageOfHexValue("00FF00"));
    assertEquals(0, colorInCombinationRepository.countUsageOfHexValue("FFFFFF"));

    // Add duplicate color to test counting
    ColorCombination combination3 = new ColorCombination("Test Combination 3", 2);
    entityManager.persistAndFlush(combination3);

    ColorInCombination duplicateColor = new ColorInCombination("FF0000", 1);
    duplicateColor.setCombination(combination3);
    ColorInCombination anotherColor = new ColorInCombination("ABCDEF", 2);
    anotherColor.setCombination(combination3);

    entityManager.persistAndFlush(duplicateColor);
    entityManager.persistAndFlush(anotherColor);

    assertEquals(2, colorInCombinationRepository.countUsageOfHexValue("FF0000"));
  }

  @Test
  @DisplayName("Should find most used colors")
  void shouldFindMostUsedColors() {
    // Add some duplicate colors to test
    ColorCombination combination3 = new ColorCombination("Test Combination 3", 2);
    entityManager.persistAndFlush(combination3);

    ColorInCombination duplicateColor = new ColorInCombination("FF0000", 1); // Duplicate
    duplicateColor.setCombination(combination3);
    ColorInCombination newColor = new ColorInCombination("ABCDEF", 2); // New color
    newColor.setCombination(combination3);

    entityManager.persistAndFlush(duplicateColor);
    entityManager.persistAndFlush(newColor);

    // When
    List<Object[]> mostUsedColors = colorInCombinationRepository.findMostUsedColors();

    // Then
    assertFalse(mostUsedColors.isEmpty());
    // FF0000 should be the most used (appears twice)
    Object[] mostUsed = mostUsedColors.get(0);
    assertEquals("FF0000", mostUsed[0]);
    assertEquals(2L, mostUsed[1]);
  }

  @Test
  @DisplayName("Should find colors created after specific date")
  void shouldFindByCreatedAfter() {
    // Given
    LocalDateTime cutoffDate = LocalDateTime.now().minusMinutes(30);

    // When
    List<ColorInCombination> colors = colorInCombinationRepository.findByCreatedAfter(cutoffDate);

    // Then
    assertEquals(4, colors.size()); // All colors should be found as they were just created

    // Test with future date
    cutoffDate = LocalDateTime.now().plusHours(1);
    colors = colorInCombinationRepository.findByCreatedAfter(cutoffDate);
    assertTrue(colors.isEmpty());
  }

  @Test
  @DisplayName("Should save and retrieve color correctly")
  void shouldSaveAndRetrieveColor() {
    // Given
    ColorCombination newCombination = new ColorCombination("New Combination", 2);
    entityManager.persistAndFlush(newCombination);

    ColorInCombination newColor = new ColorInCombination("ABCDEF", 1);
    newColor.setCombination(newCombination);

    // When
    ColorInCombination saved = colorInCombinationRepository.save(newColor);
    Optional<ColorInCombination> retrieved = colorInCombinationRepository.findById(saved.getId());

    // Then
    assertTrue(retrieved.isPresent());
    assertEquals("ABCDEF", retrieved.get().getHexValue());
    assertEquals(1, retrieved.get().getPosition());
    assertEquals(newCombination.getId(), retrieved.get().getCombination().getId());
  }

  @Test
  @DisplayName("Should verify cascade delete works through parent entity")
  void shouldVerifyCascadeDeleteWorksCorrectly() {
    // Given
    Long combinationId = combination1.getId();
    long initialColorCount = colorInCombinationRepository.countByCombinationId(combinationId);
    assertTrue(initialColorCount > 0, "Should have colors in combination1");

    // When we delete through the entity manager (simulating cascade delete)
    ColorCombination toDelete = entityManager.find(ColorCombination.class, combinationId);
    assertNotNull(toDelete);
    entityManager.remove(toDelete);
    entityManager.flush();

    // Then
    assertEquals(0, colorInCombinationRepository.countByCombinationId(combinationId));
    // Verify that colors from other combinations are still there
    assertTrue(colorInCombinationRepository.countByCombinationId(combination2.getId()) > 0);
  }

  @Test
  @DisplayName("Should find duplicate hex values")
  void shouldFindDuplicateHexValues() {
    // Given - Add duplicate color (need to persist combination first, then add colors)
    ColorCombination combination3 = new ColorCombination("Test Combination 3", 2);
    entityManager.persistAndFlush(combination3);

    ColorInCombination duplicateColor = new ColorInCombination("FF0000", 1);
    duplicateColor.setCombination(combination3);
    ColorInCombination uniqueColor = new ColorInCombination("ABCDEF", 2);
    uniqueColor.setCombination(combination3);

    entityManager.persistAndFlush(duplicateColor);
    entityManager.persistAndFlush(uniqueColor);

    // When
    List<ColorInCombination> duplicates = colorInCombinationRepository.findDuplicateHexValues();

    // Then
    assertEquals(2, duplicates.size()); // Both FF0000 colors should be returned
    assertTrue(duplicates.stream().allMatch(c -> c.getHexValue().equals("FF0000")));
  }

  @Test
  @DisplayName("Should handle empty results gracefully")
  void shouldHandleEmptyResultsGracefully() {
    // Test various methods with non-existing data
    assertTrue(colorInCombinationRepository.findByHexValue("NONEXIST").isEmpty());
    assertTrue(colorInCombinationRepository.findByCombinationIdOrderByPosition(999L).isEmpty());
    assertFalse(colorInCombinationRepository.findByCombinationIdAndPosition(999L, 1).isPresent());
    assertEquals(0, colorInCombinationRepository.countByCombinationId(999L));
    assertFalse(colorInCombinationRepository.existsByCombinationIdAndPosition(999L, 1));
  }
}
