/* (c) 2026 Alejandro Lopez Monzon <alejandro@kreaker.dev> for Kreaker Developments */
package dev.kreaker.kolors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import dev.kreaker.kolors.config.TestConfig;
import dev.kreaker.kolors.dto.ColorCombinationForm;
import dev.kreaker.kolors.service.ColorCombinationService;
import dev.kreaker.kolors.service.PerformanceMonitoringService;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public class PerformanceIntegrationTest {

   @Autowired
   private ColorCombinationService colorCombinationService;

   @Autowired
   private PerformanceMonitoringService performanceMonitoringService;

   @Nested
   class BulkOperationsPerformanceTests {

      @Test
      public void bulkInsertPerformanceTest() {
         int numberOfInserts = 100;
         StopWatch stopWatch = new StopWatch();
         stopWatch.start();

         for (int i = 0; i < numberOfInserts; i++) {
            ColorCombinationForm form = new ColorCombinationForm();
            form.getColors().clear(); // Clear default empty color
            form.setName("Bulk Combination " + i);
            // Add 5 colors
            for (int j = 0; j < 5; j++) {
               form.addColor("FF5733");
            }
            colorCombinationService.createCombination(form);
         }

         stopWatch.stop();
         long totalTimeMillis = stopWatch.getTotalTimeMillis();

         log.info("Time taken for " + numberOfInserts + " inserts: " + totalTimeMillis + "ms");
         assertThat(totalTimeMillis).isLessThan(5000); // Expect less than 5 seconds
      }
   }

   @Nested
   class SearchPerformanceTests {

      @Test
      public void searchPerformanceTest() {
         // Seed data
         for (int i = 0; i < 50; i++) {
            ColorCombinationForm form = new ColorCombinationForm();
            form.getColors().clear(); // Clear default empty color
            form.setName("Searchable Combination " + i);
            // Add 3 colors
            for (int j = 0; j < 3; j++) {
               form.addColor("FF5733");
            }
            colorCombinationService.createCombination(form);
         }

         StopWatch stopWatch = new StopWatch();
         stopWatch.start();

         colorCombinationService.searchCombinations("Searchable", null);

         stopWatch.stop();
         long totalTimeMillis = stopWatch.getTotalTimeMillis();

         log.info("Time taken for search: " + totalTimeMillis + "ms");
         assertThat(totalTimeMillis).isLessThan(1000); // Expect less than 1 second
      }
   }
}
