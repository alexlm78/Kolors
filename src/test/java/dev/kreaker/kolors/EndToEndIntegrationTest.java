package dev.kreaker.kolors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import dev.kreaker.kolors.service.ColorCombinationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Comprehensive End-to-End Integration Tests Tests complete user workflows with real database
 * interactions
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("End-to-End Integration Tests")
class EndToEndIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private ColorCombinationService colorCombinationService;

  @Autowired private ColorCombinationRepository colorCombinationRepository;

  @Autowired private ColorInCombinationRepository colorInCombinationRepository;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    // Clean database before each test
    colorInCombinationRepository.deleteAll();
    colorCombinationRepository.deleteAll();
  }

  @Nested
  @DisplayName("Complete User Journey - Create, View, Edit, Delete")
  class CompleteUserJourneyTests {

    @Test
    @DisplayName("Should complete full CRUD lifecycle for color combination")
    void shouldCompleteFullCrudLifecycle() throws Exception {
      // Step 1: Create a new combination
      mockMvc
          .perform(
              post("/combinations/create")
                  .param("name", "Sunset Palette")
                  .param("colorCount", "3")
                  .param("colors[0].hexValue", "FF6B35")
                  .param("colors[0].position", "1")
                  .param("colors[1].hexValue", "F7931E")
                  .param("colors[1].position", "2")
                  .param("colors[2].hexValue", "FFD23F")
                  .param("colors[2].position", "3"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/"))
          .andExpect(flash().attributeExists("success"));

      // Verify creation in database
      List<ColorCombination> combinations = colorCombinationRepository.findAll();
      assertThat(combinations).hasSize(1);
      ColorCombination created = combinations.get(0);
      assertThat(created.getName()).isEqualTo("Sunset Palette");
      assertThat(created.getColorCount()).isEqualTo(3);
      assertThat(created.getColors()).hasSize(3);

      Long combinationId = created.getId();

      // Step 2: View the combination
      mockMvc
          .perform(get("/combinations/" + combinationId))
          .andExpect(status().isOk())
          .andExpect(view().name("combinations/view"))
          .andExpect(model().attributeExists("combination"));

      // Step 3: Edit the combination
      mockMvc
          .perform(get("/combinations/" + combinationId + "/edit"))
          .andExpect(status().isOk())
          .andExpect(view().name("combinations/edit"))
          .andExpect(model().attributeExists("combinationForm"))
          .andExpect(model().attribute("isEditing", true));

      // Step 4: Update the combination
      mockMvc
          .perform(
              post("/combinations/" + combinationId + "/update")
                  .param("name", "Updated Sunset Palette")
                  .param("colorCount", "3")
                  .param("colors[0].hexValue", "FF6B35")
                  .param("colors[0].position", "1")
                  .param("colors[1].hexValue", "F7931E")
                  .param("colors[1].position", "2")
                  .param("colors[2].hexValue", "FFD23F")
                  .param("colors[2].position", "3"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/"))
          .andExpect(flash().attributeExists("success"));

      // Verify update in database
      Optional<ColorCombination> updated = colorCombinationRepository.findById(combinationId);
      assertThat(updated).isPresent();
      assertThat(updated.get().getName()).isEqualTo("Updated Sunset Palette");

      // Step 5: Delete confirmation
      mockMvc
          .perform(get("/combinations/" + combinationId + "/confirm-delete"))
          .andExpect(status().isOk())
          .andExpect(view().name("combinations/confirm-delete"))
          .andExpect(model().attributeExists("combination"));

      // Step 6: Delete the combination
      mockMvc
          .perform(post("/combinations/" + combinationId + "/delete"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/"))
          .andExpect(flash().attributeExists("success"));

      // Verify deletion in database
      Optional<ColorCombination> deleted = colorCombinationRepository.findById(combinationId);
      assertThat(deleted).isEmpty();

      // Verify cascade deletion of colors
      List<ColorInCombination> remainingColors = colorInCombinationRepository.findAll();
      assertThat(remainingColors).isEmpty();
    }
  }

  @Nested
  @DisplayName("Dynamic Color Management End-to-End")
  class DynamicColorManagementE2ETests {

    @Test
    @DisplayName("Should handle complete dynamic color addition and removal workflow")
    void shouldHandleCompleteDynamicColorWorkflow() throws Exception {
      // Step 1: Create initial combination with 1 color
      mockMvc
          .perform(
              post("/combinations/create")
                  .param("name", "Growing Palette")
                  .param("colorCount", "1")
                  .param("colors[0].hexValue", "FF0000")
                  .param("colors[0].position", "1"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/"));

      ColorCombination combination = colorCombinationRepository.findAll().get(0);
      Long combinationId = combination.getId();

      // Verify initial state
      assertThat(combination.getColors()).hasSize(1);
      assertThat(combination.getColorCount()).isEqualTo(1);

      // Step 2: Add second color
      mockMvc
          .perform(
              post("/combinations/" + combinationId + "/add-color")
                  .param("hexValue", "00FF00")
                  .param("position", "2"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/" + combinationId + "/edit"))
          .andExpect(flash().attributeExists("success"));

      // Verify addition
      combination = colorCombinationRepository.findById(combinationId).orElseThrow();
      assertThat(combination.getColors()).hasSize(2);
      assertThat(combination.getColorCount()).isEqualTo(2);

      // Step 3: Add third color
      mockMvc
          .perform(
              post("/combinations/" + combinationId + "/add-color")
                  .param("hexValue", "0000FF")
                  .param("position", "3"))
          .andExpect(status().is3xxRedirection())
          .andExpect(flash().attributeExists("success"));

      // Verify addition
      combination = colorCombinationRepository.findById(combinationId).orElseThrow();
      assertThat(combination.getColors()).hasSize(3);
      assertThat(combination.getColorCount()).isEqualTo(3);

      // Step 4: Remove middle color (position 2)
      mockMvc
          .perform(post("/combinations/" + combinationId + "/remove-color/2"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/" + combinationId + "/edit"))
          .andExpect(flash().attributeExists("success"));

      // Verify removal and position reordering
      combination = colorCombinationRepository.findById(combinationId).orElseThrow();
      assertThat(combination.getColors()).hasSize(2);
      assertThat(combination.getColorCount()).isEqualTo(2);

      // Verify positions are reordered correctly
      List<ColorInCombination> colors =
          colorInCombinationRepository.findByCombinationIdOrderByPosition(combinationId);
      assertThat(colors.get(0).getPosition()).isEqualTo(1);
      assertThat(colors.get(0).getHexValue()).isEqualTo("FF0000");
      assertThat(colors.get(1).getPosition()).isEqualTo(2);
      assertThat(colors.get(1).getHexValue()).isEqualTo("0000FF");

      // Step 5: Try to remove last color (should fail)
      mockMvc
          .perform(post("/combinations/" + combinationId + "/remove-color/1"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/" + combinationId + "/edit"))
          .andExpect(flash().attributeExists("error"));

      // Verify last color wasn't removed
      combination = colorCombinationRepository.findById(combinationId).orElseThrow();
      assertThat(combination.getColors()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle adding multiple colors sequentially")
    void shouldHandleAddingMultipleColorsSequentially() throws Exception {
      // Create initial combination
      mockMvc
          .perform(
              post("/combinations/create")
                  .param("name", "Sequential Palette")
                  .param("colorCount", "1")
                  .param("colors[0].hexValue", "111111")
                  .param("colors[0].position", "1"))
          .andExpect(status().is3xxRedirection());

      ColorCombination combination = colorCombinationRepository.findAll().get(0);
      Long combinationId = combination.getId();

      // Add colors sequentially up to 6 colors
      String[] colorsToAdd = {"222222", "333333", "444444", "555555", "666666"};

      for (int i = 0; i < colorsToAdd.length; i++) {
        int expectedPosition = i + 2; // Starting from position 2
        mockMvc
            .perform(
                post("/combinations/" + combinationId + "/add-color")
                    .param("hexValue", colorsToAdd[i])
                    .param("position", String.valueOf(expectedPosition)))
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attributeExists("success"));

        // Verify each addition
        combination = colorCombinationRepository.findById(combinationId).orElseThrow();
        assertThat(combination.getColors()).hasSize(i + 2);
        assertThat(combination.getColorCount()).isEqualTo(i + 2);
      }

      // Final verification - should have 6 colors total
      combination = colorCombinationRepository.findById(combinationId).orElseThrow();
      assertThat(combination.getColors()).hasSize(6);

      // Verify all positions are correct
      List<ColorInCombination> colors =
          colorInCombinationRepository.findByCombinationIdOrderByPosition(combinationId);
      for (int i = 0; i < colors.size(); i++) {
        assertThat(colors.get(i).getPosition()).isEqualTo(i + 1);
      }
    }
  }

  @Nested
  @DisplayName("Search and Filter End-to-End")
  class SearchAndFilterE2ETests {

    @Test
    @DisplayName("Should handle complete search and filter workflow")
    void shouldHandleCompleteSearchAndFilterWorkflow() throws Exception {
      // Create test data
      createTestCombination("Ocean Breeze", "0077BE", "87CEEB");
      createTestCombination("Sunset Glow", "FF6B35", "F7931E", "FFD23F");
      createTestCombination("Forest Green", "228B22", "32CD32");
      createTestCombination("Ocean Deep", "006994", "0077BE");

      // Test search by name
      mockMvc
          .perform(get("/combinations/").param("search", "Ocean"))
          .andExpect(status().isOk())
          .andExpect(model().attributeExists("combinations"))
          .andExpect(model().attribute("search", "Ocean"));

      // Test search by color count
      mockMvc
          .perform(get("/combinations/").param("colorCount", "3"))
          .andExpect(status().isOk())
          .andExpect(model().attributeExists("combinations"))
          .andExpect(model().attribute("colorCount", 3));

      // Test search by hex value
      mockMvc
          .perform(get("/combinations/").param("hexValue", "0077BE"))
          .andExpect(status().isOk())
          .andExpect(model().attributeExists("combinations"))
          .andExpect(model().attribute("hexValue", "0077BE"));

      // Test AJAX search endpoint
      mockMvc
          .perform(get("/combinations/search").param("term", "Forest"))
          .andExpect(status().isOk())
          .andExpect(model().attributeExists("combinations"));

      // Test combined search criteria
      mockMvc
          .perform(get("/combinations/").param("search", "Ocean").param("colorCount", "2"))
          .andExpect(status().isOk())
          .andExpect(model().attributeExists("combinations"));
    }

    private void createTestCombination(String name, String... hexValues) throws Exception {
      StringBuilder params = new StringBuilder();
      params.append("name=").append(name);
      params.append("&colorCount=").append(hexValues.length);

      for (int i = 0; i < hexValues.length; i++) {
        params.append("&colors[").append(i).append("].hexValue=").append(hexValues[i]);
        params.append("&colors[").append(i).append("].position=").append(i + 1);
      }

      mockMvc
          .perform(post("/combinations/create?" + params))
          .andExpect(status().is3xxRedirection());
    }
  }

  @Nested
  @DisplayName("Error Handling End-to-End")
  class ErrorHandlingE2ETests {

    @Test
    @DisplayName("Should handle validation errors throughout user workflow")
    void shouldHandleValidationErrorsThroughoutWorkflow() throws Exception {
      // Test creation with invalid data
      mockMvc
          .perform(
              post("/combinations/create")
                  .param("name", "ab") // Too short
                  .param("colorCount", "1")
                  .param("colors[0].hexValue", "INVALID") // Invalid hex
                  .param("colors[0].position", "1"))
          .andExpect(status().isOk())
          .andExpect(model().attributeExists("error"));

      // Test creation with empty name
      mockMvc
          .perform(
              post("/combinations/create")
                  .param("name", "")
                  .param("colorCount", "1")
                  .param("colors[0].hexValue", "FF0000")
                  .param("colors[0].position", "1"))
          .andExpect(status().isOk())
          .andExpect(model().attributeExists("error"));

      // Test creation with no colors
      mockMvc
          .perform(
              post("/combinations/create").param("name", "Valid Name").param("colorCount", "0"))
          .andExpect(status().isOk())
          .andExpect(model().attributeExists("error"));

      // Test accessing non-existent combination
      mockMvc
          .perform(get("/combinations/999"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/"))
          .andExpect(flash().attributeExists("error"));

      // Test editing non-existent combination
      mockMvc
          .perform(get("/combinations/999/edit"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/"))
          .andExpect(flash().attributeExists("error"));

      // Test deleting non-existent combination
      mockMvc
          .perform(post("/combinations/999/delete"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/"))
          .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("Should handle dynamic color management errors")
    void shouldHandleDynamicColorManagementErrors() throws Exception {
      // Create a valid combination first
      mockMvc
          .perform(
              post("/combinations/create")
                  .param("name", "Test Combination")
                  .param("colorCount", "1")
                  .param("colors[0].hexValue", "FF0000")
                  .param("colors[0].position", "1"))
          .andExpect(status().is3xxRedirection());

      ColorCombination combination = colorCombinationRepository.findAll().get(0);
      Long combinationId = combination.getId();

      // Test adding invalid color
      mockMvc
          .perform(
              post("/combinations/" + combinationId + "/add-color")
                  .param("hexValue", "INVALID")
                  .param("position", "2"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/" + combinationId + "/edit"))
          .andExpect(flash().attributeExists("error"));

      // Test adding color to non-existent combination
      mockMvc
          .perform(
              post("/combinations/999/add-color")
                  .param("hexValue", "00FF00")
                  .param("position", "2"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/"))
          .andExpect(flash().attributeExists("error"));

      // Test removing color from non-existent combination
      mockMvc
          .perform(post("/combinations/999/remove-color/1"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/"))
          .andExpect(flash().attributeExists("error"));

      // Test removing non-existent position
      mockMvc
          .perform(post("/combinations/" + combinationId + "/remove-color/999"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/combinations/" + combinationId + "/edit"))
          .andExpect(flash().attributeExists("error"));
    }
  }

  @Nested
  @DisplayName("Data Consistency End-to-End")
  class DataConsistencyE2ETests {

    @Test
    @DisplayName("Should maintain data consistency during complex operations")
    void shouldMaintainDataConsistencyDuringComplexOperations() throws Exception {
      // Create combination with multiple colors
      mockMvc
          .perform(
              post("/combinations/create")
                  .param("name", "Consistency Test")
                  .param("colorCount", "4")
                  .param("colors[0].hexValue", "FF0000")
                  .param("colors[0].position", "1")
                  .param("colors[1].hexValue", "00FF00")
                  .param("colors[1].position", "2")
                  .param("colors[2].hexValue", "0000FF")
                  .param("colors[2].position", "3")
                  .param("colors[3].hexValue", "FFFF00")
                  .param("colors[3].position", "4"))
          .andExpect(status().is3xxRedirection());

      ColorCombination combination = colorCombinationRepository.findAll().get(0);
      Long combinationId = combination.getId();

      // Perform multiple operations and verify consistency
      // Remove middle color (position 2)
      mockMvc
          .perform(post("/combinations/" + combinationId + "/remove-color/2"))
          .andExpect(status().is3xxRedirection())
          .andExpect(flash().attributeExists("success"));

      // Verify positions are reordered correctly
      List<ColorInCombination> colors =
          colorInCombinationRepository.findByCombinationIdOrderByPosition(combinationId);
      assertThat(colors).hasSize(3);
      assertThat(colors.get(0).getPosition()).isEqualTo(1);
      assertThat(colors.get(0).getHexValue()).isEqualTo("FF0000");
      assertThat(colors.get(1).getPosition()).isEqualTo(2);
      assertThat(colors.get(1).getHexValue()).isEqualTo("0000FF");
      assertThat(colors.get(2).getPosition()).isEqualTo(3);
      assertThat(colors.get(2).getHexValue()).isEqualTo("FFFF00");

      // Add new color
      mockMvc
          .perform(
              post("/combinations/" + combinationId + "/add-color")
                  .param("hexValue", "FF00FF")
                  .param("position", "4"))
          .andExpect(status().is3xxRedirection())
          .andExpect(flash().attributeExists("success"));

      // Verify addition
      colors = colorInCombinationRepository.findByCombinationIdOrderByPosition(combinationId);
      assertThat(colors).hasSize(4);
      assertThat(colors.get(3).getHexValue()).isEqualTo("FF00FF");
      assertThat(colors.get(3).getPosition()).isEqualTo(4);

      // Update combination
      mockMvc
          .perform(
              post("/combinations/" + combinationId + "/update")
                  .param("name", "Updated Consistency Test")
                  .param("colorCount", "4")
                  .param("colors[0].hexValue", "FF0000")
                  .param("colors[0].position", "1")
                  .param("colors[1].hexValue", "0000FF")
                  .param("colors[1].position", "2")
                  .param("colors[2].hexValue", "FFFF00")
                  .param("colors[2].position", "3")
                  .param("colors[3].hexValue", "FF00FF")
                  .param("colors[3].position", "4"))
          .andExpect(status().is3xxRedirection())
          .andExpect(flash().attributeExists("success"));

      // Final verification
      combination = colorCombinationRepository.findById(combinationId).orElseThrow();
      assertThat(combination.getName()).isEqualTo("Updated Consistency Test");
      assertThat(combination.getColorCount()).isEqualTo(4);
      assertThat(combination.getColors()).hasSize(4);
    }
  }
}
