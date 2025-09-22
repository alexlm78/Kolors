package dev.kreaker.kolors;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import dev.kreaker.kolors.exception.ColorAdditionException;
import dev.kreaker.kolors.exception.ColorRemovalException;
import dev.kreaker.kolors.exception.EmptyCombinationException;
import dev.kreaker.kolors.service.ColorCombinationService;
import dev.kreaker.kolors.service.ColorPositionService;

/**
 * Comprehensive tests for dynamic color management functionality Tests adding,
 * removing, and reordering colors in combinations
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("Dynamic Color Management Integration Tests")
class DynamicColorManagementIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ColorCombinationService colorCombinationService;

    @Autowired
    private ColorPositionService colorPositionService;

    @Autowired
    private ColorCombinationRepository colorCombinationRepository;

    @Autowired
    private ColorInCombinationRepository colorInCombinationRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Clean database before each test
        colorInCombinationRepository.deleteAll();
        colorCombinationRepository.deleteAll();
    }

    @Nested
    @DisplayName("Color Addition Tests")
    class ColorAdditionTests {

        @Test
        @DisplayName("Should add single color to existing combination")
        void shouldAddSingleColorToExistingCombination() {
            // Given - combination with one color
            ColorCombination combination = createTestCombination("Test Combination", "FF0000");
            assertThat(combination.getColors()).hasSize(1);

            // When - add second color
            ColorForm newColor = new ColorForm("00FF00", 2);
            ColorCombination updated = colorCombinationService.addColorToCombination(combination.getId(), newColor);

            // Then
            assertThat(updated.getColors()).hasSize(2);
            assertThat(updated.getColorCount()).isEqualTo(2);

            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());
            assertThat(colors.get(0).getHexValue()).isEqualTo("FF0000");
            assertThat(colors.get(0).getPosition()).isEqualTo(1);
            assertThat(colors.get(1).getHexValue()).isEqualTo("00FF00");
            assertThat(colors.get(1).getPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should add multiple colors sequentially")
        void shouldAddMultipleColorsSequentially() {
            // Given - combination with one color
            ColorCombination combination = createTestCombination("Sequential Test", "FF0000");

            // When - add multiple colors
            String[] colorsToAdd = {"00FF00", "0000FF", "FFFF00", "FF00FF", "00FFFF"};

            for (int i = 0; i < colorsToAdd.length; i++) {
                ColorForm newColor = new ColorForm(colorsToAdd[i], i + 2);
                combination = colorCombinationService.addColorToCombination(combination.getId(), newColor);
            }

            // Then
            assertThat(combination.getColors()).hasSize(6);
            assertThat(combination.getColorCount()).isEqualTo(6);

            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());

            // Verify all colors are in correct positions
            assertThat(colors.get(0).getHexValue()).isEqualTo("FF0000");
            for (int i = 0; i < colorsToAdd.length; i++) {
                assertThat(colors.get(i + 1).getHexValue()).isEqualTo(colorsToAdd[i]);
                assertThat(colors.get(i + 1).getPosition()).isEqualTo(i + 2);
            }
        }

        @Test
        @DisplayName("Should handle adding color at specific position")
        void shouldHandleAddingColorAtSpecificPosition() {
            // Given - combination with three colors
            ColorCombination combination = createTestCombination("Position Test", "FF0000", "00FF00", "0000FF");

            // When - add color at position 2 (middle)
            ColorForm newColor = new ColorForm("FFFF00", 2);
            ColorCombination updated = colorCombinationService.addColorToCombination(combination.getId(), newColor);

            // Then
            assertThat(updated.getColors()).hasSize(4);

            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());

            // Verify positions are adjusted correctly
            assertThat(colors.get(0).getHexValue()).isEqualTo("FF0000");
            assertThat(colors.get(0).getPosition()).isEqualTo(1);
            assertThat(colors.get(1).getHexValue()).isEqualTo("FFFF00"); // New color
            assertThat(colors.get(1).getPosition()).isEqualTo(2);
            assertThat(colors.get(2).getHexValue()).isEqualTo("00FF00"); // Shifted
            assertThat(colors.get(2).getPosition()).isEqualTo(3);
            assertThat(colors.get(3).getHexValue()).isEqualTo("0000FF"); // Shifted
            assertThat(colors.get(3).getPosition()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should validate hex values when adding colors")
        void shouldValidateHexValuesWhenAddingColors() {
            // Given
            ColorCombination combination = createTestCombination("Validation Test", "FF0000");

            // When & Then - invalid hex values should throw exceptions
            assertThatThrownBy(() -> {
                ColorForm invalidColor = new ColorForm("INVALID", 2);
                colorCombinationService.addColorToCombination(combination.getId(), invalidColor);
            }).isInstanceOf(ColorAdditionException.class);

            assertThatThrownBy(() -> {
                ColorForm shortColor = new ColorForm("FF0", 2);
                colorCombinationService.addColorToCombination(combination.getId(), shortColor);
            }).isInstanceOf(ColorAdditionException.class);

            assertThatThrownBy(() -> {
                ColorForm longColor = new ColorForm("FF00000", 2);
                colorCombinationService.addColorToCombination(combination.getId(), longColor);
            }).isInstanceOf(ColorAdditionException.class);
        }

        @Test
        @DisplayName("Should handle adding colors via web interface")
        void shouldHandleAddingColorsViaWebInterface() throws Exception {
            // Given
            ColorCombination combination = createTestCombination("Web Test", "FF0000");

            // When
            mockMvc.perform(post("/combinations/" + combination.getId() + "/add-color")
                    .param("hexValue", "00FF00")
                    .param("position", "2"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/combinations/" + combination.getId() + "/edit"))
                    .andExpect(flash().attributeExists("success"));

            // Then
            ColorCombination updated = colorCombinationRepository.findById(combination.getId()).orElseThrow();
            assertThat(updated.getColors()).hasSize(2);
            assertThat(updated.getColorCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Color Removal Tests")
    class ColorRemovalTests {

        @Test
        @DisplayName("Should remove color from middle position and reorder")
        void shouldRemoveColorFromMiddlePositionAndReorder() {
            // Given - combination with four colors
            ColorCombination combination = createTestCombination("Removal Test",
                    "FF0000", "00FF00", "0000FF", "FFFF00");

            // When - remove color at position 2 (00FF00)
            ColorCombination updated = colorCombinationService.removeColorFromCombination(combination.getId(), 2);

            // Then
            assertThat(updated.getColors()).hasSize(3);
            assertThat(updated.getColorCount()).isEqualTo(3);

            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());

            // Verify positions are reordered correctly
            assertThat(colors.get(0).getHexValue()).isEqualTo("FF0000");
            assertThat(colors.get(0).getPosition()).isEqualTo(1);
            assertThat(colors.get(1).getHexValue()).isEqualTo("0000FF"); // Was position 3, now 2
            assertThat(colors.get(1).getPosition()).isEqualTo(2);
            assertThat(colors.get(2).getHexValue()).isEqualTo("FFFF00"); // Was position 4, now 3
            assertThat(colors.get(2).getPosition()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should remove last color and maintain order")
        void shouldRemoveLastColorAndMaintainOrder() {
            // Given - combination with three colors
            ColorCombination combination = createTestCombination("Last Removal",
                    "FF0000", "00FF00", "0000FF");

            // When - remove last color (position 3)
            ColorCombination updated = colorCombinationService.removeColorFromCombination(combination.getId(), 3);

            // Then
            assertThat(updated.getColors()).hasSize(2);
            assertThat(updated.getColorCount()).isEqualTo(2);

            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());

            // Verify remaining colors maintain their positions
            assertThat(colors.get(0).getHexValue()).isEqualTo("FF0000");
            assertThat(colors.get(0).getPosition()).isEqualTo(1);
            assertThat(colors.get(1).getHexValue()).isEqualTo("00FF00");
            assertThat(colors.get(1).getPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should prevent removing last remaining color")
        void shouldPreventRemovingLastRemainingColor() {
            // Given - combination with only one color
            ColorCombination combination = createTestCombination("Single Color", "FF0000");

            // When & Then - should throw exception
            assertThatThrownBy(() -> {
                colorCombinationService.removeColorFromCombination(combination.getId(), 1);
            }).isInstanceOf(EmptyCombinationException.class)
                    .hasMessageContaining("Cannot remove the last color");

            // Verify color is still there
            ColorCombination unchanged = colorCombinationRepository.findById(combination.getId()).orElseThrow();
            assertThat(unchanged.getColors()).hasSize(1);
        }

        @Test
        @DisplayName("Should handle removing non-existent position")
        void shouldHandleRemovingNonExistentPosition() {
            // Given - combination with two colors
            ColorCombination combination = createTestCombination("Position Test", "FF0000", "00FF00");

            // When & Then - should throw exception for invalid position
            assertThatThrownBy(() -> {
                colorCombinationService.removeColorFromCombination(combination.getId(), 5);
            }).isInstanceOf(ColorRemovalException.class)
                    .hasMessageContaining("Color at position 5 not found");
        }

        @Test
        @DisplayName("Should handle multiple sequential removals")
        void shouldHandleMultipleSequentialRemovals() {
            // Given - combination with six colors
            ColorCombination combination = createTestCombination("Sequential Removal",
                    "FF0000", "00FF00", "0000FF", "FFFF00", "FF00FF", "00FFFF");

            // When - remove every other color (positions 2, 4, 6 become 2, 3, 4 after reordering)
            combination = colorCombinationService.removeColorFromCombination(combination.getId(), 2); // Remove 00FF00
            combination = colorCombinationService.removeColorFromCombination(combination.getId(), 3); // Remove FFFF00 (was 4)
            combination = colorCombinationService.removeColorFromCombination(combination.getId(), 3); // Remove 00FFFF (was 6)

            // Then
            assertThat(combination.getColors()).hasSize(3);

            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());

            // Verify remaining colors
            assertThat(colors.get(0).getHexValue()).isEqualTo("FF0000");
            assertThat(colors.get(0).getPosition()).isEqualTo(1);
            assertThat(colors.get(1).getHexValue()).isEqualTo("0000FF");
            assertThat(colors.get(1).getPosition()).isEqualTo(2);
            assertThat(colors.get(2).getHexValue()).isEqualTo("FF00FF");
            assertThat(colors.get(2).getPosition()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle color removal via web interface")
        void shouldHandleColorRemovalViaWebInterface() throws Exception {
            // Given
            ColorCombination combination = createTestCombination("Web Removal", "FF0000", "00FF00", "0000FF");

            // When - remove middle color
            mockMvc.perform(post("/combinations/" + combination.getId() + "/remove-color/2"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/combinations/" + combination.getId() + "/edit"))
                    .andExpect(flash().attributeExists("success"));

            // Then
            ColorCombination updated = colorCombinationRepository.findById(combination.getId()).orElseThrow();
            assertThat(updated.getColors()).hasSize(2);
            assertThat(updated.getColorCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle error when removing last color via web interface")
        void shouldHandleErrorWhenRemovingLastColorViaWebInterface() throws Exception {
            // Given
            ColorCombination combination = createTestCombination("Web Error", "FF0000");

            // When - try to remove last color
            mockMvc.perform(post("/combinations/" + combination.getId() + "/remove-color/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/combinations/" + combination.getId() + "/edit"))
                    .andExpect(flash().attributeExists("error"));

            // Then - color should still be there
            ColorCombination unchanged = colorCombinationRepository.findById(combination.getId()).orElseThrow();
            assertThat(unchanged.getColors()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Position Management Tests")
    class PositionManagementTests {

        @Test
        @DisplayName("Should calculate next available position correctly")
        void shouldCalculateNextAvailablePositionCorrectly() {
            // Given - combination with three colors
            ColorCombination combination = createTestCombination("Position Calc", "FF0000", "00FF00", "0000FF");

            // When
            Integer nextPosition = colorPositionService.getNextAvailablePosition(combination.getId());

            // Then
            assertThat(nextPosition).isEqualTo(4);
        }

        @Test
        @DisplayName("Should reorder positions after removal correctly")
        void shouldReorderPositionsAfterRemovalCorrectly() {
            // Given - combination with five colors
            ColorCombination combination = createTestCombination("Reorder Test",
                    "FF0000", "00FF00", "0000FF", "FFFF00", "FF00FF");

            // When - remove color at position 2
            colorPositionService.reorderPositionsAfterRemoval(combination.getId(), 2);

            // Then
            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());

            // Verify positions are sequential
            for (int i = 0; i < colors.size(); i++) {
                assertThat(colors.get(i).getPosition()).isEqualTo(i + 1);
            }
        }

        @Test
        @DisplayName("Should handle position gaps correctly")
        void shouldHandlePositionGapsCorrectly() {
            // Given - manually create colors with gaps in positions
            ColorCombination combination = createTestCombination("Gap Test", "FF0000");

            // Manually add colors with gaps
            ColorInCombination color2 = new ColorInCombination("00FF00", 3); // Skip position 2
            ColorInCombination color3 = new ColorInCombination("0000FF", 5); // Skip position 4

            combination.addColor(color2);
            combination.addColor(color3);
            colorCombinationRepository.save(combination);

            // When - fix sequential positions
            colorPositionService.fixSequentialPositions(combination.getId());

            // Then
            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());

            assertThat(colors).hasSize(3);
            assertThat(colors.get(0).getPosition()).isEqualTo(1);
            assertThat(colors.get(1).getPosition()).isEqualTo(2);
            assertThat(colors.get(2).getPosition()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Concurrent Operations Tests")
    class ConcurrentOperationsTests {

        @Test
        @DisplayName("Should handle concurrent color additions safely")
        void shouldHandleConcurrentColorAdditionsSafely() throws Exception {
            // Given - combination with one color
            ColorCombination combination = createTestCombination("Concurrent Test", "FF0000");
            ExecutorService executor = Executors.newFixedThreadPool(5);

            // When - add colors concurrently
            List<CompletableFuture<Void>> futures = IntStream.range(0, 10)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                try {
                    ColorForm newColor = new ColorForm(String.format("%06X", (i + 1) * 1111), i + 2);
                    colorCombinationService.addColorToCombination(combination.getId(), newColor);
                } catch (Exception e) {
                    // Some operations may fail due to concurrency, which is expected
                }
            }, executor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Then - verify data integrity
            ColorCombination updated = colorCombinationRepository.findById(combination.getId()).orElseThrow();
            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());

            // Should have at least the original color, positions should be sequential
            assertThat(colors.size()).isGreaterThanOrEqualTo(1);
            assertThat(updated.getColorCount()).isEqualTo(colors.size());

            for (int i = 0; i < colors.size(); i++) {
                assertThat(colors.get(i).getPosition()).isEqualTo(i + 1);
            }

            executor.shutdown();
        }

        @Test
        @DisplayName("Should handle concurrent color removals safely")
        void shouldHandleConcurrentColorRemovalsSafely() throws Exception {
            // Given - combination with many colors
            ColorCombination combination = createTestCombination("Concurrent Removal");
            for (int i = 1; i <= 10; i++) {
                ColorForm color = new ColorForm(String.format("%06X", i * 1111), i + 1);
                combination = colorCombinationService.addColorToCombination(combination.getId(), color);
            }

            ExecutorService executor = Executors.newFixedThreadPool(5);

            // When - remove colors concurrently (always remove position 2 to avoid conflicts)
            final Long combinationIdFinal = combination.getId();
            List<CompletableFuture<Void>> futures = IntStream.range(0, 5)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                try {
                    colorCombinationService.removeColorFromCombination(combinationIdFinal, 2);
                } catch (Exception e) {
                    // Some operations may fail due to concurrency or business rules
                }
            }, executor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Then - verify data integrity
            ColorCombination updated = colorCombinationRepository.findById(combination.getId()).orElseThrow();
            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());

            // Should have at least one color remaining, positions should be sequential
            assertThat(colors.size()).isGreaterThanOrEqualTo(1);
            assertThat(updated.getColorCount()).isEqualTo(colors.size());

            for (int i = 0; i < colors.size(); i++) {
                assertThat(colors.get(i).getPosition()).isEqualTo(i + 1);
            }

            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle adding duplicate colors")
        void shouldHandleAddingDuplicateColors() {
            // Given - combination with one color
            ColorCombination combination = createTestCombination("Duplicate Test", "FF0000");

            // When - add same color again
            ColorForm duplicateColor = new ColorForm("FF0000", 2);
            ColorCombination updated = colorCombinationService.addColorToCombination(combination.getId(), duplicateColor);

            // Then - should allow duplicates
            assertThat(updated.getColors()).hasSize(2);
            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());
            assertThat(colors.get(0).getHexValue()).isEqualTo("FF0000");
            assertThat(colors.get(1).getHexValue()).isEqualTo("FF0000");
        }

        @Test
        @DisplayName("Should handle very large number of colors")
        void shouldHandleVeryLargeNumberOfColors() {
            // Given - combination with one color
            ColorCombination combination = createTestCombination("Large Test", "FF0000");

            // When - add many colors
            for (int i = 2; i <= 100; i++) {
                ColorForm newColor = new ColorForm(String.format("%06X", i * 1000), i);
                combination = colorCombinationService.addColorToCombination(combination.getId(), newColor);
            }

            // Then
            assertThat(combination.getColors()).hasSize(100);
            assertThat(combination.getColorCount()).isEqualTo(100);

            // Verify all positions are sequential
            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());
            for (int i = 0; i < colors.size(); i++) {
                assertThat(colors.get(i).getPosition()).isEqualTo(i + 1);
            }
        }

        @Test
        @DisplayName("Should handle operations on non-existent combination")
        void shouldHandleOperationsOnNonExistentCombination() {
            // When & Then - operations on non-existent combination should throw exceptions
            assertThatThrownBy(() -> {
                ColorForm color = new ColorForm("FF0000", 1);
                colorCombinationService.addColorToCombination(999L, color);
            }).isInstanceOf(ColorAdditionException.class);

            assertThatThrownBy(() -> {
                colorCombinationService.removeColorFromCombination(999L, 1);
            }).isInstanceOf(ColorRemovalException.class);
        }
    }

    private ColorCombination createTestCombination(String name, String... hexValues) {
        ColorCombinationForm form = new ColorCombinationForm(name);
        for (String hexValue : hexValues) {
            form.addColor(hexValue);
        }
        return colorCombinationService.createCombination(form);
    }
}
