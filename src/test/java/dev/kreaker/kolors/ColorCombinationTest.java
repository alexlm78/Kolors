package dev.kreaker.kolors;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ColorCombinationTest {

    private Validator validator;
    private ColorCombination colorCombination;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        colorCombination = new ColorCombination();
    }

    @Test
    @DisplayName("Should create ColorCombination with valid data")
    void shouldCreateColorCombinationWithValidData() {
        // Given
        String name = "Test Combination";
        Integer colorCount = 3;

        // When
        ColorCombination combination = new ColorCombination(name, colorCount);

        // Then
        assertNotNull(combination);
        assertEquals(name, combination.getName());
        assertEquals(colorCount, combination.getColorCount());
        assertNotNull(combination.getCreatedAt());
        assertNotNull(combination.getColors());
        assertTrue(combination.getColors().isEmpty());
    }

    @Test
    @DisplayName("Should set createdAt automatically on creation")
    void shouldSetCreatedAtAutomatically() {
        // Given
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        // When
        ColorCombination combination = new ColorCombination("Test", 2);

        // Then
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertNotNull(combination.getCreatedAt());
        assertTrue(combination.getCreatedAt().isAfter(before));
        assertTrue(combination.getCreatedAt().isBefore(after));
    }

    @Test
    @DisplayName("Should validate name is not blank")
    void shouldValidateNameIsNotBlank() {
        // Given
        colorCombination.setName("");
        colorCombination.setColorCount(1);

        // When
        Set<ConstraintViolation<ColorCombination>> violations = validator.validate(colorCombination);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Name is required")));
    }

    @Test
    @DisplayName("Should validate name length constraints")
    void shouldValidateNameLengthConstraints() {
        // Given - name too short
        colorCombination.setName("ab");
        colorCombination.setColorCount(2);

        // When
        Set<ConstraintViolation<ColorCombination>> violations = validator.validate(colorCombination);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Name must be between 3 and 100 characters")));

        // Given - name too long
        colorCombination.setName("a".repeat(101));

        // When
        violations = validator.validate(colorCombination);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Name must be between 3 and 100 characters")));
    }

    @Test
    @DisplayName("Should validate colorCount is not null")
    void shouldValidateColorCountIsNotNull() {
        // Given
        colorCombination.setName("Valid Name");
        colorCombination.setColorCount(null);

        // When
        Set<ConstraintViolation<ColorCombination>> violations = validator.validate(colorCombination);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Must have at least one color")));
    }

    @Test
    @DisplayName("Should validate colorCount range")
    void shouldValidateColorCountRange() {
        // Given - colorCount too low (0 colors)
        colorCombination.setName("Valid Name");
        colorCombination.setColorCount(0);

        // When
        Set<ConstraintViolation<ColorCombination>> violations = validator.validate(colorCombination);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Minimum 1 color")));

        // Given - valid colorCount (1 color is now valid)
        colorCombination.setColorCount(1);

        // When
        violations = validator.validate(colorCombination);

        // Then - should be valid now (no violations for colorCount)
        assertTrue(violations.stream()
                .noneMatch(v -> v.getMessage().contains("Minimum 1 color")));
    }

    @Test
    @DisplayName("Should pass validation with valid data")
    void shouldPassValidationWithValidData() {
        // Given
        colorCombination.setName("Valid Combination");
        colorCombination.setColorCount(3);

        // When
        Set<ConstraintViolation<ColorCombination>> violations = validator.validate(colorCombination);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should manage bidirectional relationship with colors")
    void shouldManageBidirectionalRelationshipWithColors() {
        // Given
        ColorCombination combination = new ColorCombination("Test", 2);
        ColorInCombination color1 = new ColorInCombination("FF0000", 1);
        ColorInCombination color2 = new ColorInCombination("00FF00", 2);

        // When
        combination.addColor(color1);
        combination.addColor(color2);

        // Then
        assertEquals(2, combination.getColors().size());
        assertEquals(combination, color1.getCombination());
        assertEquals(combination, color2.getCombination());
    }

    @Test
    @DisplayName("Should remove color and maintain bidirectional relationship")
    void shouldRemoveColorAndMaintainBidirectionalRelationship() {
        // Given
        ColorCombination combination = new ColorCombination("Test", 2);
        ColorInCombination color1 = new ColorInCombination("FF0000", 1);
        ColorInCombination color2 = new ColorInCombination("00FF00", 2);
        combination.addColor(color1);
        combination.addColor(color2);

        // When
        combination.removeColor(color1);

        // Then
        assertEquals(1, combination.getColors().size());
        assertNull(color1.getCombination());
        assertEquals(combination, color2.getCombination());
    }

    @Test
    @DisplayName("Should have meaningful toString representation")
    void shouldHaveMeaningfulToStringRepresentation() {
        // Given
        ColorCombination combination = new ColorCombination("Test Combination", 3);

        // When
        String toString = combination.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("Test Combination"));
        assertTrue(toString.contains("3"));
        assertTrue(toString.contains("ColorCombination"));
    }
}
