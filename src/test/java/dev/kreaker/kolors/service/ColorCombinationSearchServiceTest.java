package dev.kreaker.kolors.service;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dev.kreaker.kolors.ColorCombination;
import dev.kreaker.kolors.ColorCombinationRepository;
import dev.kreaker.kolors.ColorInCombination;
import dev.kreaker.kolors.ColorInCombinationRepository;
import dev.kreaker.kolors.exception.InvalidColorFormatException;

/**
 * Tests for search functionality in ColorCombinationService
 */
@ExtendWith(MockitoExtension.class)
class ColorCombinationSearchServiceTest {

    @Mock
    private ColorCombinationRepository colorCombinationRepository;

    @Mock
    private ColorInCombinationRepository colorInCombinationRepository;

    @Mock
    private ColorPositionService colorPositionService;

    private ColorCombinationService colorCombinationService;

    private ColorCombination testCombination1;
    private ColorCombination testCombination2;
    private ColorCombination testCombination3;

    @BeforeEach
    void setUp() {
        colorCombinationService = new ColorCombinationService(
                colorCombinationRepository,
                colorInCombinationRepository,
                colorPositionService
        );

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
    void testSearchCombinations_WithNameOnly() {
        // Given
        String searchTerm = "Ocean";
        when(colorCombinationRepository.findByNameContainingIgnoreCase(searchTerm))
                .thenReturn(Arrays.asList(testCombination2));

        // When
        List<ColorCombination> results = colorCombinationService.searchCombinations(searchTerm, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Ocean Blues");
        verify(colorCombinationRepository).findByNameContainingIgnoreCase(searchTerm);
    }

    @Test
    void testSearchCombinations_WithColorCountOnly() {
        // Given
        Integer colorCount = 3;
        when(colorCombinationRepository.findByColorCount(colorCount))
                .thenReturn(Arrays.asList(testCombination1));

        // When
        List<ColorCombination> results = colorCombinationService.searchCombinations(null, colorCount);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Sunset Colors");
        verify(colorCombinationRepository).findByColorCount(colorCount);
    }

    @Test
    void testSearchCombinations_WithNameAndColorCount() {
        // Given
        String searchTerm = "Colors";
        Integer colorCount = 3;
        when(colorCombinationRepository.findSimilarCombinations(searchTerm, colorCount))
                .thenReturn(Arrays.asList(testCombination1));

        // When
        List<ColorCombination> results = colorCombinationService.searchCombinations(searchTerm, colorCount);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Sunset Colors");
        verify(colorCombinationRepository).findSimilarCombinations(searchTerm, colorCount);
    }

    @Test
    void testSearchCombinations_WithNoFilters() {
        // Given
        when(colorCombinationRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(Arrays.asList(testCombination1, testCombination2, testCombination3));

        // When
        List<ColorCombination> results = colorCombinationService.searchCombinations(null, null);

        // Then
        assertThat(results).hasSize(3);
        verify(colorCombinationRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void testFindByHexValue_ValidHex() {
        // Given
        String hexValue = "FF5733";
        when(colorCombinationRepository.findByContainingHexValue(hexValue))
                .thenReturn(Arrays.asList(testCombination1));

        // When
        List<ColorCombination> results = colorCombinationService.findByHexValue(hexValue);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Sunset Colors");
        verify(colorCombinationRepository).findByContainingHexValue(hexValue);
    }

    @Test
    void testFindByHexValue_InvalidHex() {
        // Given
        String invalidHex = "GGGGGG";

        // When & Then
        assertThatThrownBy(() -> colorCombinationService.findByHexValue(invalidHex))
                .isInstanceOf(InvalidColorFormatException.class);
    }

    @Test
    void testSearchWithFilters_HexValueOnly() {
        // Given
        String hexValue = "0077BE";
        when(colorCombinationRepository.findByContainingHexValue(hexValue))
                .thenReturn(Arrays.asList(testCombination2));

        // When
        List<ColorCombination> results = colorCombinationService.searchWithFilters(null, null, null, hexValue);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Ocean Blues");
        verify(colorCombinationRepository).findByContainingHexValue(hexValue);
    }

    @Test
    void testSearchWithFilters_NameAndColorRange() {
        // Given
        String name = "Colors";
        Integer minColors = 2;
        Integer maxColors = 4;
        when(colorCombinationRepository.findByNameAndColorCountRange(name, minColors, maxColors))
                .thenReturn(Arrays.asList(testCombination1));

        // When
        List<ColorCombination> results = colorCombinationService.searchWithFilters(name, minColors, maxColors, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Sunset Colors");
        verify(colorCombinationRepository).findByNameAndColorCountRange(name, minColors, maxColors);
    }

    @Test
    void testSearchWithFilters_ColorRangeOnly() {
        // Given
        Integer minColors = 2;
        Integer maxColors = 3;
        when(colorCombinationRepository.findByColorCountBetweenOrderByCreatedAtDesc(minColors, maxColors))
                .thenReturn(Arrays.asList(testCombination1, testCombination2));

        // When
        List<ColorCombination> results = colorCombinationService.searchWithFilters(null, minColors, maxColors, null);

        // Then
        assertThat(results).hasSize(2);
        verify(colorCombinationRepository).findByColorCountBetweenOrderByCreatedAtDesc(minColors, maxColors);
    }

    @Test
    void testSearchWithFilters_NameOnly() {
        // Given
        String name = "Ocean";
        when(colorCombinationRepository.findByNameContainingIgnoreCase(name))
                .thenReturn(Arrays.asList(testCombination2));

        // When
        List<ColorCombination> results = colorCombinationService.searchWithFilters(name, null, null, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Ocean Blues");
        verify(colorCombinationRepository).findByNameContainingIgnoreCase(name);
    }

    @Test
    void testSearchWithFilters_NoFilters() {
        // Given
        when(colorCombinationRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(Arrays.asList(testCombination1, testCombination2, testCombination3));

        // When
        List<ColorCombination> results = colorCombinationService.searchWithFilters(null, null, null, null);

        // Then
        assertThat(results).hasSize(3);
        verify(colorCombinationRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void testSearchWithFiltersPaginated_HexValue() {
        // Given
        String hexValue = "228B22";
        Pageable pageable = PageRequest.of(0, 10);
        Page<ColorCombination> page = new PageImpl<>(Arrays.asList(testCombination3));
        when(colorCombinationRepository.findByContainingHexValueWithPagination(hexValue, pageable))
                .thenReturn(page);

        // When
        Page<ColorCombination> results = colorCombinationService.searchWithFilters(null, null, null, hexValue, pageable);

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Forest Greens");
        verify(colorCombinationRepository).findByContainingHexValueWithPagination(hexValue, pageable);
    }

    @Test
    void testSearchWithFiltersPaginated_ComplexFilters() {
        // Given
        String name = "Colors";
        Integer minColors = 2;
        Integer maxColors = 4;
        Pageable pageable = PageRequest.of(0, 10);
        Page<ColorCombination> page = new PageImpl<>(Arrays.asList(testCombination1));
        when(colorCombinationRepository.findWithFilters(name, minColors, maxColors, pageable))
                .thenReturn(page);

        // When
        Page<ColorCombination> results = colorCombinationService.searchWithFilters(name, minColors, maxColors, null, pageable);

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Sunset Colors");
        verify(colorCombinationRepository).findWithFilters(name, minColors, maxColors, pageable);
    }

    @Test
    void testFindByColorCountRange_ValidRange() {
        // Given
        Integer minColors = 2;
        Integer maxColors = 3;
        when(colorCombinationRepository.findByColorCountBetweenOrderByCreatedAtDesc(minColors, maxColors))
                .thenReturn(Arrays.asList(testCombination1, testCombination2));

        // When
        List<ColorCombination> results = colorCombinationService.findByColorCountRange(minColors, maxColors);

        // Then
        assertThat(results).hasSize(2);
        verify(colorCombinationRepository).findByColorCountBetweenOrderByCreatedAtDesc(minColors, maxColors);
    }

    @Test
    void testFindByColorCountRange_NullValues() {
        // When & Then
        assertThatThrownBy(() -> colorCombinationService.findByColorCountRange(null, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Both minColors and maxColors must be specified");

        assertThatThrownBy(() -> colorCombinationService.findByColorCountRange(2, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Both minColors and maxColors must be specified");
    }

    @Test
    void testFindByColorCountRange_InvalidRange() {
        // When & Then
        assertThatThrownBy(() -> colorCombinationService.findByColorCountRange(0, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid color count range");

        assertThatThrownBy(() -> colorCombinationService.findByColorCountRange(5, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid color count range");
    }

    @Test
    void testIsValidHexColor() {
        // Valid hex colors
        assertThat(colorCombinationService.isValidHexColor("FF5733")).isTrue();
        assertThat(colorCombinationService.isValidHexColor("000000")).isTrue();
        assertThat(colorCombinationService.isValidHexColor("FFFFFF")).isTrue();
        assertThat(colorCombinationService.isValidHexColor("123ABC")).isTrue();

        // Invalid hex colors
        assertThat(colorCombinationService.isValidHexColor("GGGGGG")).isFalse();
        assertThat(colorCombinationService.isValidHexColor("FF573")).isFalse(); // Too short
        assertThat(colorCombinationService.isValidHexColor("FF57333")).isFalse(); // Too long
        assertThat(colorCombinationService.isValidHexColor(null)).isFalse();
        assertThat(colorCombinationService.isValidHexColor("")).isFalse();
    }
}
