package dev.kreaker.kolors;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests for search and filtering functionality in ColorCombinationRepository
 */
@DataJpaTest
@ActiveProfiles("test")
class ColorCombinationSearchTest {

    @Autowired
    private ColorCombinationRepository colorCombinationRepository;

    @Autowired
    private ColorInCombinationRepository colorInCombinationRepository;

    private ColorCombination combination1;
    private ColorCombination combination2;
    private ColorCombination combination3;
    private ColorCombination combination4;

    @BeforeEach
    void setUp() {
        // Create test combinations with different characteristics

        // Combination 1: "Sunset Colors" with 3 colors
        combination1 = new ColorCombination("Sunset Colors", 3);
        combination1.addColor(new ColorInCombination("FF5733", 1)); // Red-orange
        combination1.addColor(new ColorInCombination("FFC300", 2)); // Yellow
        combination1.addColor(new ColorInCombination("FF8C00", 3)); // Dark orange
        combination1 = colorCombinationRepository.save(combination1);

        // Combination 2: "Ocean Blues" with 2 colors
        combination2 = new ColorCombination("Ocean Blues", 2);
        combination2.addColor(new ColorInCombination("0077BE", 1)); // Ocean blue
        combination2.addColor(new ColorInCombination("87CEEB", 2)); // Sky blue
        combination2 = colorCombinationRepository.save(combination2);

        // Combination 3: "Forest Greens" with 4 colors
        combination3 = new ColorCombination("Forest Greens", 4);
        combination3.addColor(new ColorInCombination("228B22", 1)); // Forest green
        combination3.addColor(new ColorInCombination("32CD32", 2)); // Lime green
        combination3.addColor(new ColorInCombination("006400", 3)); // Dark green
        combination3.addColor(new ColorInCombination("90EE90", 4)); // Light green
        combination3 = colorCombinationRepository.save(combination3);

        // Combination 4: "Monochrome" with 1 color
        combination4 = new ColorCombination("Monochrome", 1);
        combination4.addColor(new ColorInCombination("000000", 1)); // Black
        combination4 = colorCombinationRepository.save(combination4);
    }

    @Test
    void testFindByNameContainingIgnoreCase() {
        // Test case-insensitive name search
        List<ColorCombination> results = colorCombinationRepository.findByNameContainingIgnoreCase("ocean");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Ocean Blues");

        // Test partial match
        results = colorCombinationRepository.findByNameContainingIgnoreCase("Colors");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Sunset Colors");

        // Test no match
        results = colorCombinationRepository.findByNameContainingIgnoreCase("Purple");
        assertThat(results).isEmpty();
    }

    @Test
    void testFindByColorCount() {
        // Test finding combinations with specific color count
        List<ColorCombination> results = colorCombinationRepository.findByColorCount(2);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Ocean Blues");

        results = colorCombinationRepository.findByColorCount(3);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Sunset Colors");

        results = colorCombinationRepository.findByColorCount(5);
        assertThat(results).isEmpty();
    }

    @Test
    void testFindByColorCountBetweenOrderByCreatedAtDesc() {
        // Test finding combinations within color count range
        List<ColorCombination> results = colorCombinationRepository.findByColorCountBetweenOrderByCreatedAtDesc(2, 3);
        assertThat(results).hasSize(2);
        // Should be ordered by creation date desc (most recent first)
        assertThat(results.get(0).getName()).isEqualTo("Ocean Blues"); // Created after Sunset Colors
        assertThat(results.get(1).getName()).isEqualTo("Sunset Colors");

        results = colorCombinationRepository.findByColorCountBetweenOrderByCreatedAtDesc(1, 1);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Monochrome");

        results = colorCombinationRepository.findByColorCountBetweenOrderByCreatedAtDesc(5, 10);
        assertThat(results).isEmpty();
    }

    @Test
    void testFindByContainingHexValue() {
        // Test finding combinations containing specific hex color
        List<ColorCombination> results = colorCombinationRepository.findByContainingHexValue("FF5733");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Sunset Colors");

        results = colorCombinationRepository.findByContainingHexValue("0077BE");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Ocean Blues");

        results = colorCombinationRepository.findByContainingHexValue("FFFFFF");
        assertThat(results).isEmpty();
    }

    @Test
    void testFindByNameAndColorCountRange() {
        // Test complex search with name and color count range
        List<ColorCombination> results = colorCombinationRepository.findByNameAndColorCountRange("Colors", 2, 4);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Sunset Colors");

        results = colorCombinationRepository.findByNameAndColorCountRange("Green", 3, 5);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Forest Greens");

        results = colorCombinationRepository.findByNameAndColorCountRange("Ocean", 1, 1);
        assertThat(results).isEmpty(); // Ocean Blues has 2 colors, not 1
    }

    @Test
    void testFindWithFilters() {
        Pageable pageable = PageRequest.of(0, 10);

        // Test with name filter only
        Page<ColorCombination> results = colorCombinationRepository.findWithFilters("Colors", null, null, pageable);
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Sunset Colors");

        // Test with color count range only
        results = colorCombinationRepository.findWithFilters(null, 2, 3, pageable);
        assertThat(results.getContent()).hasSize(2);

        // Test with both name and color count range
        results = colorCombinationRepository.findWithFilters("Ocean", 1, 3, pageable);
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Ocean Blues");

        // Test with no filters (should return all)
        results = colorCombinationRepository.findWithFilters(null, null, null, pageable);
        assertThat(results.getContent()).hasSize(4);
    }

    @Test
    void testFindByContainingHexValueWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<ColorCombination> results = colorCombinationRepository.findByContainingHexValueWithPagination("228B22", pageable);
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Forest Greens");

        results = colorCombinationRepository.findByContainingHexValueWithPagination("FFFFFF", pageable);
        assertThat(results.getContent()).isEmpty();
    }

    @Test
    void testCountByColorCount() {
        // Test counting combinations by color count
        long count = colorCombinationRepository.countByColorCount(1);
        assertThat(count).isEqualTo(1);

        count = colorCombinationRepository.countByColorCount(2);
        assertThat(count).isEqualTo(1);

        count = colorCombinationRepository.countByColorCount(3);
        assertThat(count).isEqualTo(1);

        count = colorCombinationRepository.countByColorCount(4);
        assertThat(count).isEqualTo(1);

        count = colorCombinationRepository.countByColorCount(5);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testPaginationWithNameSearch() {
        // Add more combinations to test pagination
        for (int i = 1; i <= 15; i++) {
            ColorCombination combo = new ColorCombination("Test Combination " + i, 2);
            combo.addColor(new ColorInCombination("FF0000", 1));
            combo.addColor(new ColorInCombination("00FF00", 2));
            colorCombinationRepository.save(combo);
        }

        Pageable firstPage = PageRequest.of(0, 5);
        Page<ColorCombination> results = colorCombinationRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc("Test", firstPage);

        assertThat(results.getContent()).hasSize(5);
        assertThat(results.getTotalElements()).isEqualTo(15);
        assertThat(results.getTotalPages()).isEqualTo(3);
        assertThat(results.hasNext()).isTrue();
        assertThat(results.hasPrevious()).isFalse();

        Pageable secondPage = PageRequest.of(1, 5);
        results = colorCombinationRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc("Test", secondPage);

        assertThat(results.getContent()).hasSize(5);
        assertThat(results.hasNext()).isTrue();
        assertThat(results.hasPrevious()).isTrue();
    }

    @Test
    void testExistsByNameIgnoreCase() {
        // Test checking if combination name exists
        boolean exists = colorCombinationRepository.existsByNameIgnoreCase("Sunset Colors");
        assertThat(exists).isTrue();

        exists = colorCombinationRepository.existsByNameIgnoreCase("sunset colors");
        assertThat(exists).isTrue();

        exists = colorCombinationRepository.existsByNameIgnoreCase("OCEAN BLUES");
        assertThat(exists).isTrue();

        exists = colorCombinationRepository.existsByNameIgnoreCase("Non-existent");
        assertThat(exists).isFalse();
    }
}
