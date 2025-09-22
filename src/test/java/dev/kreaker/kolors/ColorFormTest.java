package dev.kreaker.kolors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ColorFormTest {

  private Validator validator;
  private ColorForm colorForm;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
    colorForm = new ColorForm();
  }

  @Test
  @DisplayName("Should create ColorForm with valid data")
  void shouldCreateColorFormWithValidData() {
    // Given
    String hexValue = "FF0000";
    Integer position = 1;

    // When
    ColorForm form = new ColorForm(hexValue, position);

    // Then
    assertNotNull(form);
    assertEquals(hexValue, form.getHexValue());
    assertEquals(position, form.getPosition());
  }

  @Test
  @DisplayName("Should validate hexValue is not blank")
  void shouldValidateHexValueIsNotBlank() {
    // Given
    colorForm.setHexValue("");
    colorForm.setPosition(1);

    // When
    Set<ConstraintViolation<ColorForm>> violations = validator.validate(colorForm);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream()
            .anyMatch(v -> v.getMessage().contains("Hexadecimal value is required")));
  }

  @Test
  @DisplayName("Should validate hexValue format")
  void shouldValidateHexValueFormat() {
    // Given - invalid characters
    colorForm.setHexValue("GGGGGG");
    colorForm.setPosition(1);

    // When
    Set<ConstraintViolation<ColorForm>> violations = validator.validate(colorForm);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("Invalid hexadecimal format")));

    // Given - wrong length
    colorForm.setHexValue("FFF");

    // When
    violations = validator.validate(colorForm);

    // Then
    assertFalse(violations.isEmpty());
  }

  @Test
  @DisplayName("Should validate position constraints")
  void shouldValidatePositionConstraints() {
    // Given - null position
    colorForm.setHexValue("FF0000");
    colorForm.setPosition(null);

    // When
    Set<ConstraintViolation<ColorForm>> violations = validator.validate(colorForm);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("La posición es obligatoria")));

    // Given - position too low
    colorForm.setPosition(0);

    // When
    violations = validator.validate(colorForm);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("La posición mínima es 1")));

    // Given - position is now unlimited, so test with valid high position
    colorForm.setPosition(5);

    // When
    violations = validator.validate(colorForm);

    // Then - should be valid now (no max position limit)
    assertTrue(violations.isEmpty());
  }

  @Test
  @DisplayName("Should pass validation with valid data")
  void shouldPassValidationWithValidData() {
    // Given
    colorForm.setHexValue("FF0000");
    colorForm.setPosition(1);

    // When
    Set<ConstraintViolation<ColorForm>> violations = validator.validate(colorForm);

    // Then
    assertTrue(violations.isEmpty());
  }

  @Test
  @DisplayName("Should format hex value with hash prefix")
  void shouldFormatHexValueWithHashPrefix() {
    // Given
    colorForm.setHexValue("FF0000");

    // When
    String formatted = colorForm.getFormattedHex();

    // Then
    assertEquals("#FF0000", formatted);

    // Given - null hex value
    colorForm.setHexValue(null);

    // When
    formatted = colorForm.getFormattedHex();

    // Then
    assertNull(formatted);
  }

  @Test
  @DisplayName("Should validate hex value correctly")
  void shouldValidateHexValueCorrectly() {
    // Given - valid hex
    colorForm.setHexValue("FF0000");

    // When & Then
    assertTrue(colorForm.isValidHex());

    // Given - invalid hex
    colorForm.setHexValue("GGGGGG");

    // When & Then
    assertFalse(colorForm.isValidHex());

    // Given - null hex
    colorForm.setHexValue(null);

    // When & Then
    assertFalse(colorForm.isValidHex());
  }

  @Test
  @DisplayName("Should convert to entity correctly")
  void shouldConvertToEntityCorrectly() {
    // Given
    colorForm.setHexValue("FF0000");
    colorForm.setPosition(1);

    // When
    ColorInCombination entity = colorForm.toEntity();

    // Then
    assertNotNull(entity);
    assertEquals("FF0000", entity.getHexValue());
    assertEquals(1, entity.getPosition());
    assertNull(entity.getCombination());
  }

  @Test
  @DisplayName("Should create from entity correctly")
  void shouldCreateFromEntityCorrectly() {
    // Given
    ColorInCombination entity = new ColorInCombination("00FF00", 2);

    // When
    ColorForm form = ColorForm.fromEntity(entity);

    // Then
    assertNotNull(form);
    assertEquals("00FF00", form.getHexValue());
    assertEquals(2, form.getPosition());

    // Given - null entity
    // When
    form = ColorForm.fromEntity(null);

    // Then
    assertNull(form);
  }

  @Test
  @DisplayName("Should have meaningful toString representation")
  void shouldHaveMeaningfulToStringRepresentation() {
    // Given
    ColorForm form = new ColorForm("FF0000", 1);

    // When
    String toString = form.toString();

    // Then
    assertNotNull(toString);
    assertTrue(toString.contains("FF0000"));
    assertTrue(toString.contains("1"));
    assertTrue(toString.contains("ColorForm"));
  }
}
