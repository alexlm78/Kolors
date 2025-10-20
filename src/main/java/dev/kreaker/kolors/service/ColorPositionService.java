package dev.kreaker.kolors.service;

import dev.kreaker.kolors.ColorInCombination;
import dev.kreaker.kolors.ColorInCombinationRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing color positions in combinations Handles dynamic position management and
 * reordering
 */
@Service
@Transactional
public class ColorPositionService {

    private static final Logger logger = LoggerFactory.getLogger(ColorPositionService.class);

    private final ColorInCombinationRepository colorInCombinationRepository;

    public ColorPositionService(ColorInCombinationRepository colorInCombinationRepository) {
        this.colorInCombinationRepository = colorInCombinationRepository;
    }

    /** Reorders positions after a color removal to ensure sequential positions */
    public void reorderPositionsAfterRemoval(Long combinationId, Integer removedPosition) {
        logger.debug(
                "Reordering positions after removal at position {} for combination ID: {}",
                removedPosition,
                combinationId);

        if (combinationId == null || removedPosition == null) {
            logger.warn("Cannot reorder positions: combinationId or removedPosition is null");
            return;
        }

        // Get all colors that need position adjustment
        List<ColorInCombination> colorsToReorder =
                colorInCombinationRepository
                        .findByCombinationIdAndPositionGreaterThanOrderByPosition(
                                combinationId, removedPosition);

        // Decrease position by 1 for each color
        for (ColorInCombination color : colorsToReorder) {
            Integer oldPosition = color.getPosition();
            color.setPosition(oldPosition - 1);
            colorInCombinationRepository.save(color);
            logger.debug(
                    "Updated color position from {} to {} for color ID: {}",
                    oldPosition,
                    color.getPosition(),
                    color.getId());
        }

        logger.info(
                "Reordered {} colors after removal at position {} for combination ID: {}",
                colorsToReorder.size(),
                removedPosition,
                combinationId);
    }

    /** Gets the next available position for a combination */
    public Integer getNextAvailablePosition(Long combinationId) {
        if (combinationId == null) {
            throw new IllegalArgumentException("Combination ID cannot be null");
        }

        return colorInCombinationRepository
                .findMaxPositionByCombinationId(combinationId)
                .map(maxPosition -> maxPosition + 1)
                .orElse(1); // If no colors exist, start with position 1
    }

    /** Validates that positions are sequential starting from 1 */
    public boolean validateSequentialPositions(List<ColorInCombination> colors) {
        if (colors == null || colors.isEmpty()) {
            return true; // Empty list is valid
        }

        // Sort by position to check sequence
        colors.sort((c1, c2) -> c1.getPosition().compareTo(c2.getPosition()));

        for (int i = 0; i < colors.size(); i++) {
            if (colors.get(i).getPosition() != i + 1) {
                logger.warn(
                        "Non-sequential position found: expected {}, got {}",
                        i + 1,
                        colors.get(i).getPosition());
                return false;
            }
        }

        return true;
    }

    /** Fixes non-sequential positions by reordering them */
    public void fixSequentialPositions(Long combinationId) {
        logger.info("Fixing sequential positions for combination ID: {}", combinationId);

        if (combinationId == null) {
            throw new IllegalArgumentException("Combination ID cannot be null");
        }

        List<ColorInCombination> colors =
                colorInCombinationRepository.findByCombinationIdOrderByPosition(combinationId);

        // Reorder positions to be sequential
        for (int i = 0; i < colors.size(); i++) {
            ColorInCombination color = colors.get(i);
            Integer expectedPosition = i + 1;

            if (!color.getPosition().equals(expectedPosition)) {
                logger.debug(
                        "Fixing position for color ID {}: {} -> {}",
                        color.getId(),
                        color.getPosition(),
                        expectedPosition);
                color.setPosition(expectedPosition);
                colorInCombinationRepository.save(color);
            }
        }

        logger.info(
                "Fixed positions for {} colors in combination ID: {}",
                colors.size(),
                combinationId);
    }

    /** Checks if a position is available in a combination */
    public boolean isPositionAvailable(Long combinationId, Integer position) {
        if (combinationId == null || position == null || position < 1) {
            return false;
        }

        return !colorInCombinationRepository.existsByCombinationIdAndPosition(
                combinationId, position);
    }

    /** Gets all used positions in a combination */
    public List<Integer> getUsedPositions(Long combinationId) {
        if (combinationId == null) {
            throw new IllegalArgumentException("Combination ID cannot be null");
        }

        return colorInCombinationRepository
                .findByCombinationIdOrderByPosition(combinationId)
                .stream()
                .map(ColorInCombination::getPosition)
                .toList();
    }
}
