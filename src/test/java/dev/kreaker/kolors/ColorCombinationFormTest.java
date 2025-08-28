package dev.kreaker.kolors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ColorCombinationFormTest {

    private Validator validator;
    private ColorCombinationForm form;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        form = new ColorCombinationForm();
    }

    @Test
    @DisplayName("Should create ColorCombinationForm with valid data")
    void shouldCreateColorCombinationFormWithValidData() {
        // Given
        String name = "Test Combination";
        Integer colorCount = 3;
        
        // When
        ColorCombinationForm combinationForm = new ColorCombinationForm(name, colorCount);
        
        // Then
        assertNotNull(combinationForm);
        assertEquals(name, combinationForm.getName());
        assertEquals(colorCount, combinationForm.getColorCount());
        assertEquals(3, combinationForm.getColors().size());
        
        // Check that colors are initialized with correct positions
        for (int i = 0; i < 3; i++) {
            assertEquals(i + 1, combinationForm.getColors().get(i).getPosition());
            assertEquals("", combinationForm.getColors().get(i).getHexValue());
        }
    }

    @Test
    @DisplayName("Should initialize colors when setting colorCount")
    void shouldInitializeColorsWhenSettingColorCount() {
        // Given
        form.setName("Test");
        
        // When
        form.setColorCount(4);
        
        // Then
        assertEquals(4, form.getColors().size());
        for (int i = 0; i < 4; i++) {
            assertEquals(i + 1, form.getColors().get(i).getPosition());
        }
    }

    @Test
    @DisplayName("Should validate name constraints")
    void shouldValidateNameConstraints() {
        // Given - blank name
        form.setName("");
        form.setColorCount(2);
        
        // When
        Set<ConstraintViolation<ColorCombinationForm>> violations = validator.validate(form);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("El nombre es obligatorio")));
        
        // Given - name too short
        form.setName("ab");
        
        // When
        violations = validator.validate(form);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("debe tener entre 3 y 100 caracteres")));
        
        // Given - name too long
        form.setName("a".repeat(101));
        
        // When
        violations = validator.validate(form);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("debe tener entre 3 y 100 caracteres")));
    }

    @Test
    @DisplayName("Should validate colorCount constraints")
    void shouldValidateColorCountConstraints() {
        // Given - null colorCount
        form.setName("Valid Name");
        form.setColorCount(null);
        
        // When
        Set<ConstraintViolation<ColorCombinationForm>> violations = validator.validate(form);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Debe especificar el número de colores")));
        
        // Given - colorCount too low
        form.setColorCount(1);
        
        // When
        violations = validator.validate(form);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Mínimo 2 colores")));
        
        // Given - colorCount too high
        form.setColorCount(5);
        
        // When
        violations = validator.validate(form);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Máximo 4 colores")));
    }

    @Test
    @DisplayName("Should validate nested color forms")
    void shouldValidateNestedColorForms() {
        // Given
        form.setName("Valid Name");
        form.setColorCount(2);
        
        List<ColorForm> colors = new ArrayList<>();
        colors.add(new ColorForm("FF0000", 1)); // Valid
        colors.add(new ColorForm("GGGGGG", 2)); // Invalid hex
        form.setColors(colors);
        
        // When
        Set<ConstraintViolation<ColorCombinationForm>> violations = validator.validate(form);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Formato hexadecimal inválido")));
    }

    @Test
    @DisplayName("Should pass validation with valid data")
    void shouldPassValidationWithValidData() {
        // Given
        form.setName("Valid Combination");
        form.setColorCount(2);
        
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
        form.setColorCount(2);
        
        // When & Then
        assertFalse(form.isValid());
        
        // Given - invalid colorCount
        form.setName("Valid Name");
        form.setColorCount(1);
        
        // When & Then
        assertFalse(form.isValid());
        
        // Given - mismatched color count
        form.setColorCount(3);
        List<ColorForm> colors = new ArrayList<>();
        colors.add(new ColorForm("FF0000", 1));
        colors.add(new ColorForm("00FF00", 2));
        form.setColors(colors); // Only 2 colors but colorCount is 3
        
        // When & Then
        assertFalse(form.isValid());
        
        // Given - invalid hex in colors
        form.setColorCount(2);
        colors.add(new ColorForm("GGGGGG", 3));
        form.setColors(colors);
        
        // When & Then
        assertFalse(form.isValid());
        
        // Given - valid form
        form.setName("Valid Name");
        form.setColorCount(2);
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
        form.setColorCount(1);
        form.setColors(null);
        
        // When
        List<String> errors = form.getValidationErrors();
        
        // Then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("nombre debe tener al menos 3 caracteres")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("entre 2 y 4 colores")));
        
        // Given - valid form
        form.setName("Valid Name");
        form.setColorCount(2);
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
        form.setColorCount(2);
        
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
        
        // Check colors
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
    @DisplayName("Should have meaningful toString representation")
    void shouldHaveMeaningfulToStringRepresentation() {
        // Given
        ColorCombinationForm combinationForm = new ColorCombinationForm("Test", 2);
        
        // When
        String toString = combinationForm.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("Test"));
        assertTrue(toString.contains("2"));
        assertTrue(toString.contains("ColorCombinationForm"));
    }
}