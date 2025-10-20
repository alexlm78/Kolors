package dev.kreaker.kolors.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.kreaker.kolors.ColorInCombination;
import dev.kreaker.kolors.ColorInCombinationRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ColorPositionService Tests")
class ColorPositionServiceTest {

    @Mock private ColorInCombinationRepository colorInCombinationRepository;

    @InjectMocks private ColorPositionService colorPositionService;

    private ColorInCombination color1;
    private ColorInCombination color2;
    private ColorInCombination color3;

    @BeforeEach
    void setUp() {
        color1 = new ColorInCombination("FF0000", 1);
        color1.setId(1L);

        color2 = new ColorInCombination("00FF00", 2);
        color2.setId(2L);

        color3 = new ColorInCombination("0000FF", 3);
        color3.setId(3L);
    }

    @Test
    @DisplayName("Should reorder positions after removal")
    void shouldReorderPositionsAfterRemoval() {
        // Given
        Long combinationId = 1L;
        Integer removedPosition = 2;

        // Mock colors that need reordering (positions 3 and above)
        List<ColorInCombination> colorsToReorder = Arrays.asList(color3);
        when(colorInCombinationRepository.findByCombinationIdAndPositionGreaterThanOrderByPosition(
                        combinationId, removedPosition))
                .thenReturn(colorsToReorder);

        // When
        colorPositionService.reorderPositionsAfterRemoval(combinationId, removedPosition);

        // Then
        verify(colorInCombinationRepository)
                .findByCombinationIdAndPositionGreaterThanOrderByPosition(
                        combinationId, removedPosition);
        verify(colorInCombinationRepository).save(color3);
        assertEquals(2, color3.getPosition()); // Position should be decreased by 1
    }

    @Test
    @DisplayName("Should handle null parameters gracefully")
    void shouldHandleNullParametersGracefully() {
        // When & Then - should not throw exceptions
        colorPositionService.reorderPositionsAfterRemoval(null, 1);
        colorPositionService.reorderPositionsAfterRemoval(1L, null);
        colorPositionService.reorderPositionsAfterRemoval(null, null);

        // Verify no repository calls were made
        verify(colorInCombinationRepository, times(0))
                .findByCombinationIdAndPositionGreaterThanOrderByPosition(any(), any());
    }

    @Test
    @DisplayName("Should get next available position")
    void shouldGetNextAvailablePosition() {
        // Given
        Long combinationId = 1L;
        when(colorInCombinationRepository.findMaxPositionByCombinationId(combinationId))
                .thenReturn(Optional.of(3));

        // When
        Integer nextPosition = colorPositionService.getNextAvailablePosition(combinationId);

        // Then
        assertEquals(4, nextPosition);
        verify(colorInCombinationRepository).findMaxPositionByCombinationId(combinationId);
    }

    @Test
    @DisplayName("Should return position 1 for empty combination")
    void shouldReturnPosition1ForEmptyCombination() {
        // Given
        Long combinationId = 1L;
        when(colorInCombinationRepository.findMaxPositionByCombinationId(combinationId))
                .thenReturn(Optional.empty());

        // When
        Integer nextPosition = colorPositionService.getNextAvailablePosition(combinationId);

        // Then
        assertEquals(1, nextPosition);
    }

    @Test
    @DisplayName("Should throw exception for null combination ID in getNextAvailablePosition")
    void shouldThrowExceptionForNullCombinationId() {
        // When & Then
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    colorPositionService.getNextAvailablePosition(null);
                });
    }

    @Test
    @DisplayName("Should validate sequential positions correctly")
    void shouldValidateSequentialPositionsCorrectly() {
        // Given - sequential positions
        List<ColorInCombination> sequentialColors = Arrays.asList(color1, color2, color3);

        // When
        boolean isValid = colorPositionService.validateSequentialPositions(sequentialColors);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should detect non-sequential positions")
    void shouldDetectNonSequentialPositions() {
        // Given - non-sequential positions
        ColorInCombination colorWithGap = new ColorInCombination("FFFF00", 5); // Gap at position 4
        List<ColorInCombination> nonSequentialColors =
                Arrays.asList(color1, color2, color3, colorWithGap);

        // When
        boolean isValid = colorPositionService.validateSequentialPositions(nonSequentialColors);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate empty list as sequential")
    void shouldValidateEmptyListAsSequential() {
        // When
        boolean isValid = colorPositionService.validateSequentialPositions(Arrays.asList());

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should fix sequential positions")
    void shouldFixSequentialPositions() {
        // Given
        Long combinationId = 1L;
        ColorInCombination colorWithWrongPosition = new ColorInCombination("FFFF00", 5);
        colorWithWrongPosition.setId(4L);

        List<ColorInCombination> colorsWithGaps =
                Arrays.asList(color1, color2, color3, colorWithWrongPosition);
        when(colorInCombinationRepository.findByCombinationIdOrderByPosition(combinationId))
                .thenReturn(colorsWithGaps);

        // When
        colorPositionService.fixSequentialPositions(combinationId);

        // Then
        verify(colorInCombinationRepository).findByCombinationIdOrderByPosition(combinationId);
        verify(colorInCombinationRepository).save(colorWithWrongPosition);
        assertEquals(4, colorWithWrongPosition.getPosition()); // Should be fixed to position 4
    }

    @Test
    @DisplayName("Should check if position is available")
    void shouldCheckIfPositionIsAvailable() {
        // Given
        Long combinationId = 1L;
        Integer position = 2;
        when(colorInCombinationRepository.existsByCombinationIdAndPosition(combinationId, position))
                .thenReturn(false);

        // When
        boolean isAvailable = colorPositionService.isPositionAvailable(combinationId, position);

        // Then
        assertTrue(isAvailable);
        verify(colorInCombinationRepository)
                .existsByCombinationIdAndPosition(combinationId, position);
    }

    @Test
    @DisplayName("Should return false for unavailable position")
    void shouldReturnFalseForUnavailablePosition() {
        // Given
        Long combinationId = 1L;
        Integer position = 2;
        when(colorInCombinationRepository.existsByCombinationIdAndPosition(combinationId, position))
                .thenReturn(true);

        // When
        boolean isAvailable = colorPositionService.isPositionAvailable(combinationId, position);

        // Then
        assertFalse(isAvailable);
    }

    @Test
    @DisplayName("Should return false for invalid parameters")
    void shouldReturnFalseForInvalidParameters() {
        // When & Then
        assertFalse(colorPositionService.isPositionAvailable(null, 1));
        assertFalse(colorPositionService.isPositionAvailable(1L, null));
        assertFalse(colorPositionService.isPositionAvailable(1L, 0));
        assertFalse(colorPositionService.isPositionAvailable(1L, -1));
    }

    @Test
    @DisplayName("Should get used positions")
    void shouldGetUsedPositions() {
        // Given
        Long combinationId = 1L;
        List<ColorInCombination> colors = Arrays.asList(color1, color2, color3);
        when(colorInCombinationRepository.findByCombinationIdOrderByPosition(combinationId))
                .thenReturn(colors);

        // When
        List<Integer> usedPositions = colorPositionService.getUsedPositions(combinationId);

        // Then
        assertEquals(Arrays.asList(1, 2, 3), usedPositions);
        verify(colorInCombinationRepository).findByCombinationIdOrderByPosition(combinationId);
    }

    @Test
    @DisplayName("Should throw exception for null combination ID in getUsedPositions")
    void shouldThrowExceptionForNullCombinationIdInGetUsedPositions() {
        // When & Then
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    colorPositionService.getUsedPositions(null);
                });
    }
}
