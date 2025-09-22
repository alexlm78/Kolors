package dev.kreaker.kolors;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import dev.kreaker.kolors.service.ColorCombinationService;

/**
 * Performance tests for critical operations Tests system behavior under load
 * and with large datasets
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("Performance Integration Tests")
class PerformanceIntegrationTest {

    @Autowired
    private ColorCombinationService colorCombinationService;

    @Autowired
    private ColorCombinationRepository colorCombinationRepository;

    @Autowired
    private ColorInCombinationRepository colorInCombinationRepository;

    private static final int LARGE_DATASET_SIZE = 1000;
    private static final int PERFORMANCE_THRESHOLD_MS = 5000; // 5 seconds max for operations
    private static final int SEARCH_THRESHOLD_MS = 1000; // 1 second max for searches

    @BeforeEach
    void setUp() {
        // Clean database before each test
        colorInCombinationRepository.deleteAll();
        colorCombinationRepository.deleteAll();
    }

    @Nested
    @DisplayName("Bulk Operations Performance")
    class BulkOperationsPerformanceTests {

        @Test
        @DisplayName("Should create large number of combinations within performance threshold")
        void shouldCreateLargeNumberOfCombinationsWithinThreshold() {
            Instant start = Instant.now();

            // Create 1000 combinations with varying numbers of colors
            for (int i = 0; i < LARGE_DATASET_SIZE; i++) {
                ColorCombinationForm form = createTestForm("Combination " + i, (i % 4) + 1);
                colorCombinationService.createCombination(form);
            }

            Duration duration = Duration.between(start, Instant.now());

            // Verify performance
            assertThat(duration.toMillis()).isLessThan(PERFORMANCE_THRESHOLD_MS);

            // Verify data integrity
            List<ColorCombination> combinations = colorCombinationRepository.findAll();
            assertThat(combinations).hasSize(LARGE_DATASET_SIZE);

            // Verify color counts are correct
            long totalColors = colorInCombinationRepository.count();
            long expectedColors = IntStream.range(0, LARGE_DATASET_SIZE)
                    .map(i -> (i % 4) + 1)
                    .sum();
            assertThat(totalColors).isEqualTo(expectedColors);
        }

        @Test
        @DisplayName("Should handle bulk deletion within performance threshold")
        void shouldHandleBulkDeletionWithinThreshold() {
            // Setup: Create test data
            List<ColorCombination> combinations = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                ColorCombinationForm form = createTestForm("ToDelete " + i, 3);
                ColorCombination combination = colorCombinationService.createCombination(form);
                combinations.add(combination);
            }

            Instant start = Instant.now();

            // Delete all combinations
            for (ColorCombination combination : combinations) {
                colorCombinationService.deleteCombination(combination.getId());
            }

            Duration duration = Duration.between(start, Instant.now());

            // Verify performance
            assertThat(duration.toMillis()).isLessThan(PERFORMANCE_THRESHOLD_MS);

            // Verify deletion
            assertThat(colorCombinationRepository.count()).isZero();
            assertThat(colorInCombinationRepository.count()).isZero();
        }

        @Test
        @DisplayName("Should handle bulk updates within performance threshold")
        void shouldHandleBulkUpdatesWithinThreshold() {
            // Setup: Create test data
            List<ColorCombination> combinations = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                ColorCombinationForm form = createTestForm("Original " + i, 2);
                ColorCombination combination = colorCombinationService.createCombination(form);
                combinations.add(combination);
            }

            Instant start = Instant.now();

            // Update all combinations
            for (ColorCombination combination : combinations) {
                ColorCombinationForm updateForm = createTestForm("Updated " + combination.getId(), 3);
                colorCombinationService.updateCombination(combination.getId(), updateForm);
            }

            Duration duration = Duration.between(start, Instant.now());

            // Verify performance
            assertThat(duration.toMillis()).isLessThan(PERFORMANCE_THRESHOLD_MS);

            // Verify updates
            List<ColorCombination> updated = colorCombinationRepository.findAll();
            assertThat(updated).allMatch(c -> c.getName().startsWith("Updated"));
            assertThat(updated).allMatch(c -> c.getColorCount() == 3);
        }
    }

    @Nested
    @DisplayName("Search Performance")
    class SearchPerformanceTests {

        @BeforeEach
        void setupLargeDataset() {
            // Create diverse dataset for search testing
            String[] namePatterns = {"Ocean", "Sunset", "Forest", "Mountain", "Desert"};
            String[] colorBases = {"FF", "00", "AA", "55", "CC"};

            for (int i = 0; i < LARGE_DATASET_SIZE; i++) {
                String name = namePatterns[i % namePatterns.length] + " " + i;
                ColorCombinationForm form = new ColorCombinationForm(name);

                int colorCount = (i % 4) + 1;
                for (int j = 0; j < colorCount; j++) {
                    String hex = colorBases[j % colorBases.length]
                            + String.format("%04X", i + j);
                    form.addColor(hex.substring(0, 6));
                }

                colorCombinationService.createCombination(form);
            }
        }

        @Test
        @DisplayName("Should search by name within performance threshold")
        void shouldSearchByNameWithinThreshold() {
            Instant start = Instant.now();

            List<ColorCombination> results = colorCombinationService.searchCombinations("Ocean", null);

            Duration duration = Duration.between(start, Instant.now());

            // Verify performance
            assertThat(duration.toMillis()).isLessThan(SEARCH_THRESHOLD_MS);

            // Verify results
            assertThat(results).isNotEmpty();
            assertThat(results).allMatch(c -> c.getName().contains("Ocean"));
        }

        @Test
        @DisplayName("Should search by color count within performance threshold")
        void shouldSearchByColorCountWithinThreshold() {
            Instant start = Instant.now();

            List<ColorCombination> results = colorCombinationService.searchCombinations(null, 3);

            Duration duration = Duration.between(start, Instant.now());

            // Verify performance
            assertThat(duration.toMillis()).isLessThan(SEARCH_THRESHOLD_MS);

            // Verify results
            assertThat(results).isNotEmpty();
            assertThat(results).allMatch(c -> c.getColorCount() == 3);
        }

        @Test
        @DisplayName("Should search by hex value within performance threshold")
        void shouldSearchByHexValueWithinThreshold() {
            Instant start = Instant.now();

            List<ColorCombination> results = colorCombinationService.findByHexValue("FF0000");

            Duration duration = Duration.between(start, Instant.now());

            // Verify performance
            assertThat(duration.toMillis()).isLessThan(SEARCH_THRESHOLD_MS);

            // Verify results (may be empty, but should complete quickly)
            assertThat(results).isNotNull();
        }

        @Test
        @DisplayName("Should handle complex search queries within performance threshold")
        void shouldHandleComplexSearchQueriesWithinThreshold() {
            Instant start = Instant.now();

            // Perform multiple search operations
            List<ColorCombination> nameResults = colorCombinationService.searchCombinations("Sunset", null);
            List<ColorCombination> countResults = colorCombinationService.searchCombinations(null, 2);
            List<ColorCombination> combinedResults = colorCombinationService.searchCombinations("Forest", 4);

            Duration duration = Duration.between(start, Instant.now());

            // Verify performance for combined operations
            assertThat(duration.toMillis()).isLessThan(SEARCH_THRESHOLD_MS * 2);

            // Verify all searches returned results
            assertThat(nameResults).isNotNull();
            assertThat(countResults).isNotNull();
            assertThat(combinedResults).isNotNull();
        }
    }

    @Nested
    @DisplayName("Dynamic Color Management Performance")
    class DynamicColorManagementPerformanceTests {

        @Test
        @DisplayName("Should handle rapid color additions within performance threshold")
        void shouldHandleRapidColorAdditionsWithinThreshold() {
            // Create initial combination
            ColorCombinationForm form = createTestForm("Performance Test", 1);
            ColorCombination combination = colorCombinationService.createCombination(form);

            Instant start = Instant.now();

            // Add 50 colors rapidly
            for (int i = 2; i <= 51; i++) {
                ColorForm colorForm = new ColorForm(String.format("%06X", i * 1000), i);
                colorCombinationService.addColorToCombination(combination.getId(), colorForm);
            }

            Duration duration = Duration.between(start, Instant.now());

            // Verify performance
            assertThat(duration.toMillis()).isLessThan(PERFORMANCE_THRESHOLD_MS);

            // Verify final state
            ColorCombination updated = colorCombinationService.getById(combination.getId());
            assertThat(updated.getColors()).hasSize(51);
            assertThat(updated.getColorCount()).isEqualTo(51);
        }

        @Test
        @DisplayName("Should handle rapid color removals with reordering within performance threshold")
        void shouldHandleRapidColorRemovalsWithinThreshold() {
            // Create combination with many colors
            ColorCombinationForm form = new ColorCombinationForm("Removal Test");
            for (int i = 1; i <= 20; i++) {
                form.addColor(String.format("%06X", i * 1111));
            }
            ColorCombination combination = colorCombinationService.createCombination(form);

            Instant start = Instant.now();

            // Remove every other color (10 removals with reordering)
            for (int i = 2; i <= 20; i += 2) {
                // Always remove position 2 since positions get reordered
                colorCombinationService.removeColorFromCombination(combination.getId(), 2);
            }

            Duration duration = Duration.between(start, Instant.now());

            // Verify performance
            assertThat(duration.toMillis()).isLessThan(PERFORMANCE_THRESHOLD_MS);

            // Verify final state
            ColorCombination updated = colorCombinationService.getById(combination.getId());
            assertThat(updated.getColors()).hasSize(10);
            assertThat(updated.getColorCount()).isEqualTo(10);

            // Verify positions are sequential
            List<ColorInCombination> colors = colorInCombinationRepository
                    .findByCombinationIdOrderByPosition(combination.getId());
            for (int i = 0; i < colors.size(); i++) {
                assertThat(colors.get(i).getPosition()).isEqualTo(i + 1);
            }
        }
    }

    @Nested
    @DisplayName("Concurrent Operations Performance")
    class ConcurrentOperationsPerformanceTests {

        @Test
        @DisplayName("Should handle concurrent combination creation")
        void shouldHandleConcurrentCombinationCreation() throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(10);

            Instant start = Instant.now();

            // Create 100 combinations concurrently (10 threads, 10 combinations each)
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (int thread = 0; thread < 10; thread++) {
                final int threadId = thread;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (int i = 0; i < 10; i++) {
                        ColorCombinationForm form = createTestForm(
                                "Concurrent " + threadId + "-" + i,
                                (i % 3) + 1
                        );
                        colorCombinationService.createCombination(form);
                    }
                }, executor);
                futures.add(future);
            }

            // Wait for all operations to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            Duration duration = Duration.between(start, Instant.now());

            // Verify performance
            assertThat(duration.toMillis()).isLessThan(PERFORMANCE_THRESHOLD_MS * 2);

            // Verify all combinations were created
            List<ColorCombination> combinations = colorCombinationRepository.findAll();
            assertThat(combinations).hasSize(100);

            executor.shutdown();
        }

        @Test
        @DisplayName("Should handle concurrent searches")
        void shouldHandleConcurrentSearches() throws Exception {
            // Setup test data
            for (int i = 0; i < 100; i++) {
                ColorCombinationForm form = createTestForm("Search Test " + i, (i % 4) + 1);
                colorCombinationService.createCombination(form);
            }

            ExecutorService executor = Executors.newFixedThreadPool(20);

            Instant start = Instant.now();

            // Perform 100 concurrent searches
            List<CompletableFuture<List<ColorCombination>>> futures = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                final int searchId = i;
                CompletableFuture<List<ColorCombination>> future = CompletableFuture.supplyAsync(() -> {
                    if (searchId % 3 == 0) {
                        return colorCombinationService.searchCombinations("Search", null);
                    } else if (searchId % 3 == 1) {
                        return colorCombinationService.searchCombinations(null, (searchId % 4) + 1);
                    } else {
                        return colorCombinationService.findByHexValue("FF0000");
                    }
                }, executor);
                futures.add(future);
            }

            // Wait for all searches to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            Duration duration = Duration.between(start, Instant.now());

            // Verify performance
            assertThat(duration.toMillis()).isLessThan(SEARCH_THRESHOLD_MS * 5);

            // Verify all searches completed successfully
            for (CompletableFuture<List<ColorCombination>> future : futures) {
                assertThat(future.get()).isNotNull();
            }

            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("Memory Usage Performance")
    class MemoryUsagePerformanceTests {

        @Test
        @DisplayName("Should handle large combinations without memory issues")
        void shouldHandleLargeCombinationsWithoutMemoryIssues() {
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();

            // Create combinations with many colors
            List<ColorCombination> largeCombinations = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                ColorCombinationForm form = new ColorCombinationForm("Large Combination " + i);
                // Add 20 colors to each combination
                for (int j = 0; j < 20; j++) {
                    form.addColor(String.format("%06X", (i * 20 + j) * 1000));
                }
                ColorCombination combination = colorCombinationService.createCombination(form);
                largeCombinations.add(combination);
            }

            // Force garbage collection
            System.gc();
            Thread.yield();

            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = finalMemory - initialMemory;

            // Verify reasonable memory usage (less than 100MB for this test)
            assertThat(memoryUsed).isLessThan(100 * 1024 * 1024);

            // Verify data integrity
            assertThat(largeCombinations).hasSize(50);
            assertThat(colorCombinationRepository.count()).isEqualTo(50);
            assertThat(colorInCombinationRepository.count()).isEqualTo(1000); // 50 * 20
        }
    }

    private ColorCombinationForm createTestForm(String name, int colorCount) {
        ColorCombinationForm form = new ColorCombinationForm(name);
        for (int i = 0; i < colorCount; i++) {
            // Generate different hex values based on position
            String hex = String.format("%02X%02X%02X",
                    (i * 50) % 256,
                    (i * 100) % 256,
                    (i * 150) % 256);
            form.addColor(hex);
        }
        return form;
    }
}
