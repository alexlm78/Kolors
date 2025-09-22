package dev.kreaker.kolors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

class ColorInCombinationTest {

  private Validator validator;
  private ColorInCombination colorInCombination;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
    colorInCombination = new ColorInCombination();
  }

  @Test
  @DisplayName("Should create ColorInCombination with valid data")
  void shouldCreateColorInCombinationWithValidData() {
    // Given
    String hexValue = "FF0000";
    Integer position = 1;

    // When
    ColorInCombination color = new ColorInCombination(hexValue, position);

    // Then
    assertNotNull(color);
    assertEquals(hexValue, color.getHexValue());
    assertEquals(position, color.getPosition());
    assertNull(color.getCombination());
  }

  @Test
  @DisplayName("Should create ColorInCombination with combination")
  void shouldCreateColorInCombinationWithCombination() {
    // Given
    String hexValue = "00FF00";
    Integer position = 2;
    ColorCombination combination = new ColorCombination("Test", 2);

    // When
    ColorInCombination color = new ColorInCombination(hexValue, position, combination);

    // Then
    assertEquals(hexValue, color.getHexValue());
    assertEquals(position, color.getPosition());
    assertEquals(combination, color.getCombination());
  }

  @Test
  @DisplayName("Should validate hexValue is not blank")
  void shouldValidateHexValueIsNotBlank() {
    // Given
    colorInCombination.setHexValue("");
    colorInCombination.setPosition(1);

    // When
    Set<ConstraintViolation<ColorInCombination>> violations =
        validator.validate(colorInCombination);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream()
            .anyMatch(v -> v.getMessage().contains("Hexadecimal value is required")));
  }

  @Test
  @DisplayName("Should validate hexValue format")
  void shouldValidateHexValueFormat() {
    // Given - invalid hex format
    colorInCombination.setHexValue("GGGGGG");
    colorInCombination.setPosition(1);

    // When
    Set<ConstraintViolation<ColorInCombination>> violations =
        validator.validate(colorInCombination);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("Invalid hexadecimal format")));

    // Given - too short
    colorInCombination.setHexValue("FFF");

    // When
    violations = validator.validate(colorInCombination);

    // Then
    assertFalse(violations.isEmpty());

    // Given - too long
    colorInCombination.setHexValue("FFFFFFF");

    // When
    violations = validator.validate(colorInCombination);

    // Then
    assertFalse(violations.isEmpty());
  }

  @Test
  @DisplayName("Should accept valid hex formats")
  void shouldAcceptValidHexFormats() {
    // Test various valid hex formats
    String[] validHexValues = {
      "FF0000", "00FF00", "0000FF", "FFFFFF", "000000", "123ABC", "abcdef"
    };

    for (String hexValue : validHexValues) {
      // Given
      colorInCombination.setHexValue(hexValue);
      colorInCombination.setPosition(1);

      // When
      Set<ConstraintViolation<ColorInCombination>> violations =
          validator.validate(colorInCombination);

      // Then
      assertTrue(violations.isEmpty(), "Should accept hex value: " + hexValue);
    }
  }

  @Test
  @DisplayName("Should validate position is not null")
  void shouldValidatePositionIsNotNull() {
    // Given
    colorInCombination.setHexValue("FF0000");
    colorInCombination.setPosition(null);

    // When
    Set<ConstraintViolation<ColorInCombination>> violations =
        validator.validate(colorInCombination);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Position is required")));
  }

  @Test
  @DisplayName("Should validate position range")
  void shouldValidatePositionRange() {
    // Given - position too low
    colorInCombination.setHexValue("FF0000");
    colorInCombination.setPosition(0);

    // When
    Set<ConstraintViolation<ColorInCombination>> violations =
        validator.validate(colorInCombination);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Minimum position is 1")));

    // Given - position is now unlimited, so test with valid high position
    colorInCombination.setPosition(5);

    // When
    violations = validator.validate(colorInCombination);

    // Then - should be valid now (no max position limit)
    assertTrue(violations.isEmpty());
  }

  @Test
  @DisplayName("Should pass validation with valid data")
  void shouldPassValidationWithValidData() {
    // Given
    colorInCombination.setHexValue("FF0000");
    colorInCombination.setPosition(1);

    // When
    Set<ConstraintViolation<ColorInCombination>> violations =
        validator.validate(colorInCombination);

    // Then
    assertTrue(violations.isEmpty());
  }

  @Test
  @DisplayName("Should format hex value with hash prefix")
  void shouldFormatHexValueWithHashPrefix() {
    // Given
    colorInCombination.setHexValue("FF0000");

    // When
    String formatted = colorInCombination.getFormattedHex();

    // Then
    assertEquals("#FF0000", formatted);
  }

  @Test
  @DisplayName("Should validate hex value correctly")
  void shouldValidateHexValueCorrectly() {
    // Given - valid hex
    colorInCombination.setHexValue("FF0000");

    // When & Then
    assertTrue(colorInCombination.isValidHex());

    // Given - invalid hex
    colorInCombination.setHexValue("GGGGGG");

    // When & Then
    assertFalse(colorInCombination.isValidHex());

    // Given - null hex
    colorInCombination.setHexValue(null);

    // When & Then
    assertFalse(colorInCombination.isValidHex());
  }

  @Test
  @DisplayName("Should implement equals and hashCode correctly")
  void shouldImplementEqualsAndHashCodeCorrectly() {
    // Given
    ColorInCombination color1 = new ColorInCombination("FF0000", 1);
    ColorInCombination color2 = new ColorInCombination("FF0000", 1);
    ColorInCombination color3 = new ColorInCombination("00FF00", 1);
    ColorInCombination color4 = new ColorInCombination("FF0000", 2);

    // When & Then
    assertEquals(color1, color2);
    assertEquals(color1.hashCode(), color2.hashCode());

    assertNotEquals(color1, color3);
    assertNotEquals(color1, color4);

    // Test with IDs
    color1.setId(1L);
    color2.setId(1L);
    assertEquals(color1, color2);

    color2.setId(2L);
    assertNotEquals(color1, color2);
  }

  @Test
  @DisplayName("Should have meaningful toString representation")
  void shouldHaveMeaningfulToStringRepresentation() {
    // Given
    ColorInCombination color = new ColorInCombination("FF0000", 1);

    // When
    String toString = color.toString();

    // Then
    assertNotNull(toString);
    assertTrue(toString.contains("FF0000"));
    assertTrue(toString.contains("1"));
    assertTrue(toString.contains("ColorInCombination"));
  }
}
