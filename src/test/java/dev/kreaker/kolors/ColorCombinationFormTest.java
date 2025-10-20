package dev.kreaker.kolors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ColorCombinationFormTest {

    private Validator validator;
    private ColorCombinationForm form;

    @Test
    @DisplayName("Should create ColorCombinationForm with default single color")
    void shouldCreateColorCombinationFormWithDefaultSingleColor() {
        // When
        ColorCombinationForm combinationForm = new ColorCombinationForm();

        // Then
        assertNotNull(combinationForm);
        assertEquals(1, combinationForm.getColorCount()); // Default starts with 1 color
        assertEquals(1, combinationForm.getColors().size());

        // Check that the default color is initialized correctly
        ColorForm color = combinationForm.getColors().get(0);
        assertEquals(1, color.getPosition());
        assertEquals("", color.getHexValue());
    }

    @Test
    @DisplayName("Should create ColorCombinationForm with name")
    void shouldCreateColorCombinationFormWithName() {
        // Given
        String name = "Test Combination";

        // When
        ColorCombinationForm combinationForm = new ColorCombinationForm(name);

        // Then
        assertNotNull(combinationForm);
        assertEquals(name, combinationForm.getName());
        assertEquals(1, combinationForm.getColorCount()); // Default starts with 1 color
        assertEquals(1, combinationForm.getColors().size());
    }

    @Test
    @DisplayName("Should add colors dynamically")
    void shouldAddColorsDynamically() {
        // Given
        form.setName("Test");

        // When - add colors
        form.addColor("FF0000");
        form.addColor("00FF00");

        // Then
        assertEquals(3, form.getColors().size()); // 1 default + 2 added
        assertEquals(3, form.getColorCount());

        // Check positions are sequential
        assertEquals(1, form.getColors().get(0).getPosition());
        assertEquals(2, form.getColors().get(1).getPosition());
        assertEquals(3, form.getColors().get(2).getPosition());

        // Check hex values
        assertEquals("", form.getColors().get(0).getHexValue()); // Default empty
        assertEquals("FF0000", form.getColors().get(1).getHexValue());
        assertEquals("00FF00", form.getColors().get(2).getHexValue());
    }

    @Test
    @DisplayName("Should remove colors dynamically")
    void shouldRemoveColorsDynamically() {
        // Given
        form.setName("Test");
        form.addColor("FF0000");
        form.addColor("00FF00");
        form.addColor("0000FF");
        assertEquals(4, form.getColors().size());

        // When - remove color at index 1
        form.removeColor(1);

        // Then
        assertEquals(3, form.getColors().size());
        assertEquals(3, form.getColorCount());

        // Check positions are reordered
        assertEquals(1, form.getColors().get(0).getPosition());
        assertEquals(2, form.getColors().get(1).getPosition());
        assertEquals(3, form.getColors().get(2).getPosition());
    }

    @Test
    @DisplayName("Should not remove last color")
    void shouldNotRemoveLastColor() {
        // Given - form with only one color
        assertEquals(1, form.getColors().size());

        // When - try to remove the only color
        form.removeColor(0);

        // Then - should still have one color
        assertEquals(1, form.getColors().size());
        assertEquals(1, form.getColorCount());
    }

    @Test
    @DisplayName("Should validate name constraints")
    void shouldValidateNameConstraints() {
        // Given - blank name
        form.setName("");

        // When
        Set<ConstraintViolation<ColorCombinationForm>> violations = validator.validate(form);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Name is required")));

        // Given - name too short
        form.setName("ab");

        // When
        violations = validator.validate(form);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getMessage()
                                                .contains(
                                                        "Name must be between 3 and 100 characters")));

        // Given - name too long
        form.setName("a".repeat(101));

        // When
        violations = validator.validate(form);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getMessage()
                                                .contains(
                                                        "Name must be between 3 and 100 characters")));
    }

    @Test
    @DisplayName("Should validate colors list is not empty")
    void shouldValidateColorsListIsNotEmpty() {
        // Given - empty colors list
        form.setName("Valid Name");
        form.setColors(new ArrayList<>());

        // When
        Set<ConstraintViolation<ColorCombinationForm>> violations = validator.validate(form);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getMessage().contains("Must have at least one color")));
    }

    @Test
    @DisplayName("Should validate nested color forms")
    void shouldValidateNestedColorForms() {
        // Given
        form.setName("Valid Name");

        List<ColorForm> colors = new ArrayList<>();
        colors.add(new ColorForm("FF0000", 1)); // Valid
        colors.add(new ColorForm("GGGGGG", 2)); // Invalid hex
        form.setColors(colors);

        // When
        Set<ConstraintViolation<ColorCombinationForm>> violations = validator.validate(form);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getMessage().contains("Invalid hexadecimal format")));
    }

    @Test
    @DisplayName("Should pass validation with valid data")
    void shouldPassValidationWithValidData() {
        // Given
        form.setName("Valid Combination");

        List<ColorForm> colors = new ArrayList<>();
        colors.add(new ColorForm("FF0000", 1));
        colors.add(new ColorForm("00FF00", 2));
        form.setColors(colors);

        // When
        Set<ConstraintViolation<ColorCombinationForm>> violations = validator.validate(form);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should validate form correctly with isValid method")
    void shouldValidateFormCorrectlyWithIsValidMethod() {
        // Given - invalid name
        form.setName("ab");

        // When & Then
        assertFalse(form.isValid());

        // Given - empty colors
        form.setName("Valid Name");
        form.setColors(new ArrayList<>());

        // When & Then
        assertFalse(form.isValid());

        // Given - invalid hex in colors
        List<ColorForm> colors = new ArrayList<>();
        colors.add(new ColorForm("GGGGGG", 1)); // Invalid hex
        form.setColors(colors);

        // When & Then
        assertFalse(form.isValid());

        // Given - valid form
        form.setName("Valid Name");
        colors = new ArrayList<>();
        colors.add(new ColorForm("FF0000", 1));
        colors.add(new ColorForm("00FF00", 2));
        form.setColors(colors);

        // When & Then
        assertTrue(form.isValid());
    }

    @Test
    @DisplayName("Should provide validation errors")
    void shouldProvideValidationErrors() {
        // Given - multiple validation errors
        form.setName("ab");
        form.setColors(new ArrayList<>());

        // When
        List<String> errors = form.getValidationErrors();

        // Then
        assertFalse(errors.isEmpty());
        assertTrue(
                errors.stream().anyMatch(e -> e.contains("Name must have at least 3 characters")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Must have at least one color")));

        // Given - valid form
        form.setName("Valid Name");
        List<ColorForm> colors = new ArrayList<>();
        colors.add(new ColorForm("FF0000", 1));
        colors.add(new ColorForm("00FF00", 2));
        form.setColors(colors);

        // When
        errors = form.getValidationErrors();

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Should convert to entity correctly")
    void shouldConvertToEntityCorrectly() {
        // Given
        form.setName("Test Combination");

        List<ColorForm> colors = new ArrayList<>();
        colors.add(new ColorForm("FF0000", 1));
        colors.add(new ColorForm("00FF00", 2));
        form.setColors(colors);

        // When
        ColorCombination entity = form.toEntity();

        // Then
        assertNotNull(entity);
        assertEquals("Test Combination", entity.getName());
        assertEquals(2, entity.getColorCount());
        assertEquals(2, entity.getColors().size());

        // Check colors
        ColorInCombination color1 = entity.getColors().get(0);
        assertEquals("FF0000", color1.getHexValue());
        assertEquals(1, color1.getPosition());
        assertEquals(entity, color1.getCombination());

        ColorInCombination color2 = entity.getColors().get(1);
        assertEquals("00FF00", color2.getHexValue());
        assertEquals(2, color2.getPosition());
        assertEquals(entity, color2.getCombination());
    }

    @Test
    @DisplayName("Should create from entity correctly")
    void shouldCreateFromEntityCorrectly() {
        // Given
        ColorCombination entity = new ColorCombination("Test Combination", 2);
        entity.addColor(new ColorInCombination("FF0000", 1));
        entity.addColor(new ColorInCombination("00FF00", 2));

        // When
        ColorCombinationForm formFromEntity = ColorCombinationForm.fromEntity(entity);

        // Then
        assertNotNull(formFromEntity);
        assertEquals("Test Combination", formFromEntity.getName());
        assertEquals(2, formFromEntity.getColorCount());
        assertEquals(2, formFromEntity.getColors().size());

        // Check colors are sorted by position
        assertEquals("FF0000", formFromEntity.getColors().get(0).getHexValue());
        assertEquals(1, formFromEntity.getColors().get(0).getPosition());
        assertEquals("00FF00", formFromEntity.getColors().get(1).getHexValue());
        assertEquals(2, formFromEntity.getColors().get(1).getPosition());

        // Given - null entity
        // When
        formFromEntity = ColorCombinationForm.fromEntity(null);

        // Then
        assertNull(formFromEntity);
    }

    @Test
    @DisplayName("Should handle empty entity colors gracefully")
    void shouldHandleEmptyEntityColorsGracefully() {
        // Given - entity with no colors
        ColorCombination entity = new ColorCombination("Test Combination", 0);

        // When
        ColorCombinationForm formFromEntity = ColorCombinationForm.fromEntity(entity);

        // Then
        assertNotNull(formFromEntity);
        assertEquals("Test Combination", formFromEntity.getName());
        assertEquals(1, formFromEntity.getColorCount()); // Should have default color
        assertEquals(1, formFromEntity.getColors().size());
        assertEquals("", formFromEntity.getColors().get(0).getHexValue());
        assertEquals(1, formFromEntity.getColors().get(0).getPosition());
    }

    @Test
    @DisplayName("Should handle null colors list gracefully")
    void shouldHandleNullColorsListGracefully() {
        // Given
        form.setColors(null);

        // When
        form.setColors(null);

        // Then
        assertNotNull(form.getColors());
        assertTrue(form.getColors().isEmpty());
    }

    @Test
    @DisplayName("Should reorder positions when setting colors")
    void shouldReorderPositionsWhenSettingColors() {
        // Given - colors with non-sequential positions
        List<ColorForm> colors = new ArrayList<>();
        colors.add(new ColorForm("FF0000", 5));
        colors.add(new ColorForm("00FF00", 3));
        colors.add(new ColorForm("0000FF", 1));

        // When
        form.setColors(colors);

        // Then - positions should be reordered to 1, 2, 3
        assertEquals(1, form.getColors().get(0).getPosition());
        assertEquals(2, form.getColors().get(1).getPosition());
        assertEquals(3, form.getColors().get(2).getPosition());
    }

    @Test
    @DisplayName("Should have meaningful toString representation")
    void shouldHaveMeaningfulToStringRepresentation() {
        // Given
        ColorCombinationForm combinationForm = new ColorCombinationForm("Test");

        // When
        String toString = combinationForm.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("Test"));
        assertTrue(toString.contains("1")); // colorCount
        assertTrue(toString.contains("ColorCombinationForm"));
    }
}
