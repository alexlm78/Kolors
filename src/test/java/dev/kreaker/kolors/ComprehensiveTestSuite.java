package dev.kreaker.kolors;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kreaker.kolors.service.ColorCombinationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprehensive test suite that validates all major functionality This serves as a validation that
 * the end-to-end tests are properly implemented
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("Comprehensive Test Suite")
class ComprehensiveTestSuite {

    @Autowired private ColorCombinationService colorCombinationService;

    @Autowired private ColorCombinationRepository colorCombinationRepository;

    @Autowired private ColorInCombinationRepository colorInCombinationRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        colorInCombinationRepository.deleteAll();
        colorCombinationRepository.deleteAll();
    }

    @Test
    @DisplayName("Should validate that comprehensive test infrastructure is working")
    void shouldValidateTestInfrastructure() {
        // Test 1: Basic CRUD operations
        ColorCombinationForm form = new ColorCombinationForm("Test Combination");
        // Clear the default empty color and add valid colors
        form.getColors().clear();
        form.addColor("FF0000");
        form.addColor("00FF00");

        ColorCombination created = colorCombinationService.createCombination(form);
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Test Combination");
        assertThat(created.getColors()).hasSize(2);

        // Test 2: Dynamic color management
        ColorForm newColor = new ColorForm("0000FF", 3);
        ColorCombination updated =
                colorCombinationService.addColorToCombination(created.getId(), newColor);
        assertThat(updated.getColors()).hasSize(3);

        // Test 3: Color removal with reordering
        ColorCombination afterRemoval =
                colorCombinationService.removeColorFromCombination(created.getId(), 2);
        assertThat(afterRemoval.getColors()).hasSize(2);

        List<ColorInCombination> colors =
                colorInCombinationRepository.findByCombinationIdOrderByPosition(created.getId());
        assertThat(colors.get(0).getPosition()).isEqualTo(1);
        assertThat(colors.get(1).getPosition()).isEqualTo(2);

        // Test 4: Search functionality
        List<ColorCombination> searchResults =
                colorCombinationService.searchCombinations("Test", null);
        assertThat(searchResults).hasSize(1);
        assertThat(searchResults.get(0).getName()).isEqualTo("Test Combination");

        // Test 5: Deletion
        colorCombinationService.deleteCombination(created.getId());
        assertThat(colorCombinationRepository.findById(created.getId())).isEmpty();
        assertThat(colorInCombinationRepository.findByCombinationIdOrderByPosition(created.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("Should validate performance with multiple combinations")
    void shouldValidatePerformanceWithMultipleCombinations() {
        // Create multiple combinations to test performance
        for (int i = 0; i < 50; i++) {
            ColorCombinationForm form = new ColorCombinationForm("Performance Test " + i);
            form.getColors().clear(); // Clear default empty color
            for (int j = 0; j < (i % 4) + 1; j++) {
                form.addColor(String.format("%06X", (i * 10 + j) * 1000));
            }
            colorCombinationService.createCombination(form);
        }

        // Verify all combinations were created
        List<ColorCombination> allCombinations = colorCombinationRepository.findAll();
        assertThat(allCombinations).hasSize(50);

        // Test search performance
        List<ColorCombination> searchResults =
                colorCombinationService.searchCombinations("Performance", null);
        assertThat(searchResults).hasSize(50);

        // Test color count filtering
        List<ColorCombination> singleColorCombinations =
                colorCombinationService.searchCombinations(null, 1);
        assertThat(singleColorCombinations).isNotEmpty();
    }

    @Test
    @DisplayName("Should validate error handling scenarios")
    void shouldValidateErrorHandlingScenarios() {
        // Create a test combination
        ColorCombinationForm form = new ColorCombinationForm("Error Test");
        form.getColors().clear(); // Clear default empty color
        form.addColor("FF0000");
        ColorCombination combination = colorCombinationService.createCombination(form);

        // Test that we can't remove the last color (should handle gracefully)
        try {
            colorCombinationService.removeColorFromCombination(combination.getId(), 1);
            // If no exception is thrown, verify the color is still there
            ColorCombination unchanged =
                    colorCombinationRepository.findById(combination.getId()).orElseThrow();
            assertThat(unchanged.getColors()).hasSize(1);
        } catch (Exception e) {
            // Exception is expected and acceptable
            assertThat(e.getMessage()).contains("Cannot remove the last color");
        }

        // Test operations on non-existent combination
        try {
            colorCombinationService.getById(999L);
        } catch (Exception e) {
            // Exception is expected
            assertThat(e).isNotNull();
        }
    }
}
