package dev.kreaker.kolors;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kreaker.kolors.service.ColorCombinationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for database indexes and EntityGraph optimizations Tests the core performance
 * improvements without aspect monitoring
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DatabaseIndexTest {

    @Autowired private ColorCombinationService colorCombinationService;

    @Autowired private ColorCombinationRepository colorCombinationRepository;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        colorCombinationRepository.deleteAll();
    }

    @Test
    void testEntityGraphOptimization() {
        // Create test data
        ColorCombination combination1 =
                createTestCombination("Test Combination 1", "FF0000", "00FF00");
        ColorCombination combination2 =
                createTestCombination("Test Combination 2", "0000FF", "FFFF00");

        colorCombinationRepository.save(combination1);
        colorCombinationRepository.save(combination2);

        // Test findAllByOrderByCreatedAtDesc with EntityGraph
        List<ColorCombination> combinations = colorCombinationService.findAllCombinations();

        assertThat(combinations).hasSize(2);

        // Verify that colors are loaded (no lazy loading exception)
        for (ColorCombination combination : combinations) {
            assertThat(combination.getColors()).isNotEmpty();
            // Access colors to ensure they're loaded
            combination
                    .getColors()
                    .forEach(
                            color -> {
                                assertThat(color.getHexValue()).isNotNull();
                                assertThat(color.getPosition()).isNotNull();
                            });
        }
    }

    @Test
    void testOptimizedFindById() {
        // Create test data
        ColorCombination combination =
                createTestCombination("Test Combination", "FF0000", "00FF00", "0000FF");
        ColorCombination saved = colorCombinationRepository.save(combination);

        // Test optimized findById
        ColorCombination found = colorCombinationService.getById(saved.getId());

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Test Combination");
        assertThat(found.getColors()).hasSize(3);

        // Verify that colors are loaded without additional queries
        found.getColors()
                .forEach(
                        color -> {
                            assertThat(color.getHexValue()).isNotNull();
                            assertThat(color.getPosition()).isNotNull();
                        });
    }

    @Test
    void testIndexedQueries() {
        // Create test data with specific patterns for index testing
        ColorCombination redCombination = createTestCombination("Red Theme", "FF0000", "FF6666");
        ColorCombination blueCombination = createTestCombination("Blue Theme", "0000FF", "6666FF");
        ColorCombination greenCombination =
                createTestCombination("Green Theme", "00FF00", "66FF66");

        colorCombinationRepository.save(redCombination);
        colorCombinationRepository.save(blueCombination);
        colorCombinationRepository.save(greenCombination);

        // Test name search (should use idx_combination_name index)
        List<ColorCombination> themeResults =
                colorCombinationRepository.findByNameContainingIgnoreCase("Theme");
        assertThat(themeResults).hasSize(3);

        // Test color count search (should use idx_combination_color_count index)
        List<ColorCombination> twoColorResults = colorCombinationRepository.findByColorCount(2);
        assertThat(twoColorResults).hasSize(3);

        // Test hex value search (should use idx_color_hex_value index)
        List<ColorCombination> redResults =
                colorCombinationRepository.findByContainingHexValue("FF0000");
        assertThat(redResults).hasSize(1);
        assertThat(redResults.get(0).getName()).isEqualTo("Red Theme");
    }

    @Test
    void testSearchPerformance() {
        // Create multiple test combinations
        for (int i = 1; i <= 10; i++) {
            ColorCombination combination =
                    createTestCombination(
                            "Combination " + i,
                            String.format("%02X0000", i * 10),
                            String.format("00%02X00", i * 10));
            colorCombinationRepository.save(combination);
        }

        // Test search by name
        List<ColorCombination> results =
                colorCombinationService.searchCombinations("Combination", null);
        assertThat(results).hasSize(10);

        // Test search by color count
        results = colorCombinationService.searchCombinations(null, 2);
        assertThat(results).hasSize(10);
    }

    @Test
    void testDatabaseIndexes() {
        // Create test data to verify indexes work
        ColorCombination combination1 = createTestCombination("Performance Test 1", "ABCDEF");
        ColorCombination combination2 =
                createTestCombination("Performance Test 2", "123456", "FEDCBA");
        ColorCombination combination3 =
                createTestCombination("Different Name", "ABCDEF", "123456", "FEDCBA");

        colorCombinationRepository.save(combination1);
        colorCombinationRepository.save(combination2);
        colorCombinationRepository.save(combination3);

        // Test name index
        List<ColorCombination> performanceResults =
                colorCombinationRepository.findByNameContainingIgnoreCase("Performance");
        assertThat(performanceResults).hasSize(2);

        // Test color count index
        List<ColorCombination> singleColorResults = colorCombinationRepository.findByColorCount(1);
        assertThat(singleColorResults).hasSize(1);

        List<ColorCombination> twoColorResults = colorCombinationRepository.findByColorCount(2);
        assertThat(twoColorResults).hasSize(1);

        List<ColorCombination> threeColorResults = colorCombinationRepository.findByColorCount(3);
        assertThat(threeColorResults).hasSize(1);

        // Test hex value index
        List<ColorCombination> abcdefResults =
                colorCombinationRepository.findByContainingHexValue("ABCDEF");
        assertThat(abcdefResults).hasSize(2);
    }

    private ColorCombination createTestCombination(String name, String... hexValues) {
        ColorCombination combination = new ColorCombination(name);

        for (int i = 0; i < hexValues.length; i++) {
            ColorInCombination color = new ColorInCombination(hexValues[i], i + 1);
            combination.addColor(color);
        }

        return combination;
    }
}
