package dev.kreaker.kolors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import dev.kreaker.kolors.service.ColorCombinationService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Tests for search and filtering functionality in ColorCombinationController */
@WebMvcTest(ColorCombinationController.class)
@ActiveProfiles("test")
class ColorCombinationSearchControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ColorCombinationService colorCombinationService;

    private ColorCombination testCombination1;
    private ColorCombination testCombination2;
    private ColorCombination testCombination3;

    @BeforeEach
    void setUp() {
        // Create test combinations
        testCombination1 = new ColorCombination("Sunset Colors", 3);
        testCombination1.addColor(new ColorInCombination("FF5733", 1));
        testCombination1.addColor(new ColorInCombination("FFC300", 2));
        testCombination1.addColor(new ColorInCombination("FF8C00", 3));

        testCombination2 = new ColorCombination("Ocean Blues", 2);
        testCombination2.addColor(new ColorInCombination("0077BE", 1));
        testCombination2.addColor(new ColorInCombination("87CEEB", 2));

        testCombination3 = new ColorCombination("Forest Greens", 4);
        testCombination3.addColor(new ColorInCombination("228B22", 1));
        testCombination3.addColor(new ColorInCombination("32CD32", 2));
        testCombination3.addColor(new ColorInCombination("006400", 3));
        testCombination3.addColor(new ColorInCombination("90EE90", 4));
    }

    @Test
    void testIndex_NoFilters() throws Exception {
        // Given
        List<ColorCombination> allCombinations =
                Arrays.asList(testCombination1, testCombination2, testCombination3);
        when(colorCombinationService.searchWithFilters(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(allCombinations);

        // When & Then
        mockMvc.perform(get("/combinations/"))
                .andExpect(status().isOk())
                .andExpect(view().name("combinations/index"))
                .andExpect(model().attribute("combinations", hasSize(3)))
                .andExpect(model().attribute("totalCombinations", 3));

        verify(colorCombinationService).searchWithFilters(isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void testIndex_WithNameSearch() throws Exception {
        // Given
        String searchTerm = "Ocean";
        List<ColorCombination> filteredCombinations = Arrays.asList(testCombination2);
        when(colorCombinationService.searchWithFilters(
                        eq(searchTerm), isNull(), isNull(), isNull()))
                .thenReturn(filteredCombinations);

        // When & Then
        mockMvc.perform(get("/combinations/").param("search", searchTerm))
                .andExpect(status().isOk())
                .andExpect(view().name("combinations/index"))
                .andExpect(model().attribute("combinations", hasSize(1)))
                .andExpect(model().attribute("search", searchTerm))
                .andExpect(model().attribute("totalCombinations", 1));

        verify(colorCombinationService)
                .searchWithFilters(eq(searchTerm), isNull(), isNull(), isNull());
    }

    @Test
    void testIndex_WithColorCountFilter() throws Exception {
        // Given
        Integer colorCount = 3;
        List<ColorCombination> filteredCombinations = Arrays.asList(testCombination1);
        when(colorCombinationService.searchWithFilters(
                        isNull(), eq(colorCount), eq(colorCount), isNull()))
                .thenReturn(filteredCombinations);

        // When & Then
        mockMvc.perform(get("/combinations/").param("colorCount", colorCount.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("combinations/index"))
                .andExpect(model().attribute("combinations", hasSize(1)))
                .andExpect(model().attribute("colorCount", colorCount))
                .andExpect(model().attribute("totalCombinations", 1));

        verify(colorCombinationService)
                .searchWithFilters(isNull(), eq(colorCount), eq(colorCount), isNull());
    }

    @Test
    void testIndex_WithHexValueSearch() throws Exception {
        // Given
        String hexValue = "FF5733";
        List<ColorCombination> filteredCombinations = Arrays.asList(testCombination1);
        when(colorCombinationService.searchWithFilters(isNull(), isNull(), isNull(), eq(hexValue)))
                .thenReturn(filteredCombinations);

        // When & Then
        mockMvc.perform(get("/combinations/").param("hexValue", hexValue))
                .andExpect(status().isOk())
                .andExpect(view().name("combinations/index"))
                .andExpect(model().attribute("combinations", hasSize(1)))
                .andExpect(model().attribute("hexValue", hexValue))
                .andExpect(model().attribute("totalCombinations", 1));

        verify(colorCombinationService)
                .searchWithFilters(isNull(), isNull(), isNull(), eq(hexValue));
    }

    @Test
    void testIndex_WithColorRange() throws Exception {
        // Given
        Integer minColors = 2;
        Integer maxColors = 3;
        List<ColorCombination> filteredCombinations =
                Arrays.asList(testCombination1, testCombination2);
        when(colorCombinationService.searchWithFilters(
                        isNull(), eq(minColors), eq(maxColors), isNull()))
                .thenReturn(filteredCombinations);

        // When & Then
        mockMvc.perform(
                        get("/combinations/")
                                .param("minColors", minColors.toString())
                                .param("maxColors", maxColors.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("combinations/index"))
                .andExpect(model().attribute("combinations", hasSize(2)))
                .andExpect(model().attribute("minColors", minColors))
                .andExpect(model().attribute("maxColors", maxColors))
                .andExpect(model().attribute("totalCombinations", 2));

        verify(colorCombinationService)
                .searchWithFilters(isNull(), eq(minColors), eq(maxColors), isNull());
    }

    @Test
    void testIndex_WithMultipleFilters() throws Exception {
        // Given
        String searchTerm = "Colors";
        Integer minColors = 2;
        Integer maxColors = 4;
        List<ColorCombination> filteredCombinations = Arrays.asList(testCombination1);
        when(colorCombinationService.searchWithFilters(
                        eq(searchTerm), eq(minColors), eq(maxColors), isNull()))
                .thenReturn(filteredCombinations);

        // When & Then
        mockMvc.perform(
                        get("/combinations/")
                                .param("search", searchTerm)
                                .param("minColors", minColors.toString())
                                .param("maxColors", maxColors.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("combinations/index"))
                .andExpect(model().attribute("combinations", hasSize(1)))
                .andExpect(model().attribute("search", searchTerm))
                .andExpect(model().attribute("minColors", minColors))
                .andExpect(model().attribute("maxColors", maxColors))
                .andExpect(model().attribute("totalCombinations", 1));

        verify(colorCombinationService)
                .searchWithFilters(eq(searchTerm), eq(minColors), eq(maxColors), isNull());
    }

    @Test
    void testSearchEndpoint_AJAX() throws Exception {
        // Given
        String searchTerm = "Ocean";
        Integer colorCount = 2;
        List<ColorCombination> filteredCombinations = Arrays.asList(testCombination2);
        when(colorCombinationService.searchWithFilters(
                        eq(searchTerm), eq(colorCount), eq(colorCount), isNull()))
                .thenReturn(filteredCombinations);

        // When & Then
        mockMvc.perform(
                        get("/combinations/search")
                                .param("term", searchTerm)
                                .param("colorCount", colorCount.toString()))
                .andExpect(status().isOk())
                .andExpect(
                        view().name("combinations/fragments/combination-list :: combinationList"))
                .andExpect(model().attribute("combinations", hasSize(1)));

        verify(colorCombinationService)
                .searchWithFilters(eq(searchTerm), eq(colorCount), eq(colorCount), isNull());
    }

    @Test
    void testSearchEndpoint_WithHexValue() throws Exception {
        // Given
        String hexValue = "228B22";
        List<ColorCombination> filteredCombinations = Arrays.asList(testCombination3);
        when(colorCombinationService.searchWithFilters(isNull(), isNull(), isNull(), eq(hexValue)))
                .thenReturn(filteredCombinations);

        // When & Then
        mockMvc.perform(get("/combinations/search").param("hexValue", hexValue))
                .andExpect(status().isOk())
                .andExpect(
                        view().name("combinations/fragments/combination-list :: combinationList"))
                .andExpect(model().attribute("combinations", hasSize(1)));

        verify(colorCombinationService)
                .searchWithFilters(isNull(), isNull(), isNull(), eq(hexValue));
    }

    @Test
    void testSearchEndpoint_WithRangeFilters() throws Exception {
        // Given
        Integer minColors = 3;
        Integer maxColors = 4;
        List<ColorCombination> filteredCombinations =
                Arrays.asList(testCombination1, testCombination3);
        when(colorCombinationService.searchWithFilters(
                        isNull(), eq(minColors), eq(maxColors), isNull()))
                .thenReturn(filteredCombinations);

        // When & Then
        mockMvc.perform(
                        get("/combinations/search")
                                .param("minColors", minColors.toString())
                                .param("maxColors", maxColors.toString()))
                .andExpect(status().isOk())
                .andExpect(
                        view().name("combinations/fragments/combination-list :: combinationList"))
                .andExpect(model().attribute("combinations", hasSize(2)));

        verify(colorCombinationService)
                .searchWithFilters(isNull(), eq(minColors), eq(maxColors), isNull());
    }

    @Test
    void testPaginatedSearch() throws Exception {
        // Given
        String searchTerm = "Test";
        Integer page = 0;
        Integer size = 5;
        List<ColorCombination> combinations = Arrays.asList(testCombination1, testCombination2);
        Page<ColorCombination> combinationPage =
                new PageImpl<>(combinations, PageRequest.of(page, size), 10);

        when(colorCombinationService.searchWithFilters(
                        eq(searchTerm), isNull(), isNull(), isNull(), any()))
                .thenReturn(combinationPage);

        // When & Then
        mockMvc.perform(
                        get("/combinations/paginated")
                                .param("search", searchTerm)
                                .param("page", page.toString())
                                .param("size", size.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("combinations/index"))
                .andExpect(model().attribute("combinations", hasSize(2)))
                .andExpect(model().attribute("search", searchTerm))
                .andExpect(model().attribute("totalCombinations", 10L))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("pageSize", size))
                .andExpect(model().attribute("hasNext", true))
                .andExpect(model().attribute("hasPrevious", false));

        verify(colorCombinationService)
                .searchWithFilters(eq(searchTerm), isNull(), isNull(), isNull(), any());
    }

    @Test
    void testPaginatedSearch_WithAllFilters() throws Exception {
        // Given
        String searchTerm = "Colors";
        Integer colorCount = 3;
        String hexValue = "FF5733";
        Integer page = 1;
        Integer size = 10;
        List<ColorCombination> combinations = Arrays.asList(testCombination1);
        Page<ColorCombination> combinationPage =
                new PageImpl<>(combinations, PageRequest.of(page, size), 1);

        when(colorCombinationService.searchWithFilters(
                        isNull(), isNull(), isNull(), eq(hexValue), any()))
                .thenReturn(combinationPage);

        // When & Then
        mockMvc.perform(
                        get("/combinations/paginated")
                                .param("search", searchTerm)
                                .param("colorCount", colorCount.toString())
                                .param("hexValue", hexValue)
                                .param("page", page.toString())
                                .param("size", size.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("combinations/index"))
                .andExpect(model().attribute("combinations", hasSize(1)))
                .andExpect(model().attribute("search", searchTerm))
                .andExpect(model().attribute("colorCount", colorCount))
                .andExpect(model().attribute("hexValue", hexValue))
                .andExpect(model().attribute("totalCombinations", 1L))
                .andExpect(model().attribute("currentPage", page))
                .andExpect(model().attribute("totalPages", 1))
                .andExpect(model().attribute("hasNext", false))
                .andExpect(model().attribute("hasPrevious", false));

        // Should search by hex value when provided (takes precedence)
        verify(colorCombinationService)
                .searchWithFilters(isNull(), isNull(), isNull(), eq(hexValue), any());
    }

    @Test
    void testIndex_ErrorHandling() throws Exception {
        // Given
        when(colorCombinationService.searchWithFilters(isNull(), isNull(), isNull(), isNull()))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/combinations/"))
                .andExpect(status().isOk())
                .andExpect(view().name("combinations/index"))
                .andExpect(
                        model().attribute(
                                        "error",
                                        containsString("Error loading color combinations")))
                .andExpect(model().attributeExists("combinations"))
                .andExpect(model().attributeExists("combinationForm"));
    }

    @Test
    void testSearchEndpoint_ErrorHandling() throws Exception {
        // Given
        when(colorCombinationService.searchWithFilters(anyString(), any(), any(), any()))
                .thenThrow(new RuntimeException("Search error"));

        // When & Then
        mockMvc.perform(get("/combinations/search").param("term", "test"))
                .andExpect(status().isOk())
                .andExpect(
                        view().name("combinations/fragments/combination-list :: combinationList"))
                .andExpect(model().attribute("error", "Search error"))
                .andExpect(model().attributeExists("combinations"));
    }
}
