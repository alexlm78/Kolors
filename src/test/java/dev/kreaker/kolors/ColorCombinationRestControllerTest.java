package dev.kreaker.kolors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.service.ColorCombinationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ColorCombinationRestController.class)
@DisplayName("ColorCombinationRestController Tests")
class ColorCombinationRestControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ColorCombinationService colorCombinationService;

  @Autowired private ObjectMapper objectMapper;

  private ColorCombination testCombination;
  private ColorForm testColorForm;

  @BeforeEach
  void setUp() {
    // Create test combination
    testCombination = new ColorCombination("Test Combination", 3);
    testCombination.setId(1L);
    testCombination.setCreatedAt(LocalDateTime.now());
    testCombination.addColor(new ColorInCombination("FF0000", 1));
    testCombination.addColor(new ColorInCombination("00FF00", 2));
    testCombination.addColor(new ColorInCombination("0000FF", 3));

    // Create test color form
    testColorForm = new ColorForm("FFFF00", 4);
  }

  @Test
  @DisplayName("Should add color via AJAX successfully")
  void shouldAddColorViaAjaxSuccessfully() throws Exception {
    // Given
    when(colorCombinationService.addColorToCombination(eq(1L), any(ColorForm.class)))
        .thenReturn(testCombination);

    // When & Then
    mockMvc
        .perform(
            post("/api/combinations/1/colors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testColorForm)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Color added successfully"))
        .andExpect(jsonPath("$.combination.id").value(1))
        .andExpect(jsonPath("$.combination.name").value("Test Combination"))
        .andExpect(jsonPath("$.combination.colorCount").value(3));
  }

  @Test
  @DisplayName("Should handle validation errors when adding color via AJAX")
  void shouldHandleValidationErrorsWhenAddingColorViaAjax() throws Exception {
    // Given - invalid color form
    ColorForm invalidColorForm = new ColorForm("INVALID", 1);

    // When & Then
    mockMvc
        .perform(
            post("/api/combinations/1/colors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidColorForm)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  @DisplayName("Should handle combination not found when adding color via AJAX")
  void shouldHandleCombinationNotFoundWhenAddingColorViaAjax() throws Exception {
    // Given
    when(colorCombinationService.addColorToCombination(eq(999L), any(ColorForm.class)))
        .thenThrow(new ColorCombinationNotFoundException("Combination not found"));

    // When & Then
    mockMvc
        .perform(
            post("/api/combinations/999/colors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testColorForm)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should remove color via AJAX successfully")
  void shouldRemoveColorViaAjaxSuccessfully() throws Exception {
    // Given
    when(colorCombinationService.removeColorFromCombination(1L, 2)).thenReturn(testCombination);

    // When & Then
    mockMvc
        .perform(delete("/api/combinations/1/colors/2"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Color removed successfully"))
        .andExpect(jsonPath("$.combination.id").value(1))
        .andExpect(jsonPath("$.combination.colorCount").value(3));
  }

  @Test
  @DisplayName("Should handle validation error when removing last color via AJAX")
  void shouldHandleValidationErrorWhenRemovingLastColorViaAjax() throws Exception {
    // Given
    when(colorCombinationService.removeColorFromCombination(1L, 1))
        .thenThrow(new ColorCombinationValidationException("Cannot remove the last color"));

    // When & Then
    mockMvc
        .perform(delete("/api/combinations/1/colors/1"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Cannot remove the last color"));
  }

  @Test
  @DisplayName("Should handle combination not found when removing color via AJAX")
  void shouldHandleCombinationNotFoundWhenRemovingColorViaAjax() throws Exception {
    // Given
    when(colorCombinationService.removeColorFromCombination(999L, 1))
        .thenThrow(new ColorCombinationNotFoundException("Combination not found"));

    // When & Then
    mockMvc.perform(delete("/api/combinations/999/colors/1")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should validate color format via AJAX")
  void shouldValidateColorFormatViaAjax() throws Exception {
    // Given
    when(colorCombinationService.isValidHexColor("FF0000")).thenReturn(true);

    ColorForm validColorForm = new ColorForm("FF0000", 1);

    // When & Then
    mockMvc
        .perform(
            post("/api/combinations/validate-color")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validColorForm)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.valid").value(true))
        .andExpect(jsonPath("$.message").value("Valid color"));
  }

  @Test
  @DisplayName("Should detect invalid color format via AJAX")
  void shouldDetectInvalidColorFormatViaAjax() throws Exception {
    // Given
    ColorForm invalidColorForm = new ColorForm("INVALID", 1);

    // When & Then
    mockMvc
        .perform(
            post("/api/combinations/validate-color")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidColorForm)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.valid").value(false))
        .andExpect(jsonPath("$.message").value("Invalid hexadecimal format"));
  }

  @Test
  @DisplayName("Should get combination details via AJAX")
  void shouldGetCombinationDetailsViaAjax() throws Exception {
    // Given
    when(colorCombinationService.getById(1L)).thenReturn(testCombination);

    // When & Then
    mockMvc
        .perform(post("/api/combinations/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.combination.id").value(1))
        .andExpect(jsonPath("$.combination.name").value("Test Combination"))
        .andExpect(jsonPath("$.combination.colorCount").value(3))
        .andExpect(jsonPath("$.combination.colors").isArray())
        .andExpect(jsonPath("$.combination.colors.length()").value(3));
  }

  @Test
  @DisplayName("Should handle combination not found when getting details via AJAX")
  void shouldHandleCombinationNotFoundWhenGettingDetailsViaAjax() throws Exception {
    // Given
    when(colorCombinationService.getById(999L))
        .thenThrow(new ColorCombinationNotFoundException("Combination not found"));

    // When & Then
    mockMvc.perform(post("/api/combinations/999")).andExpect(status().isNotFound());
  }
}
