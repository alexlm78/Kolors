package dev.kreaker.kolors;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kreaker.kolors.dto.ColorForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ColorFormTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void whenHexValueIsValid_thenNoViolations() {
        ColorForm form = new ColorForm();
        form.setHexValue("FF5733");
        form.setPosition(1);

        Set<ConstraintViolation<ColorForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    public void whenHexValueIsInvalid_thenViolation() {
        ColorForm form = new ColorForm();
        form.setHexValue("ZZZZZZ"); // Invalid hex characters
        form.setPosition(1);

        Set<ConstraintViolation<ColorForm>> violations = validator.validate(form);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Invalid hexadecimal format"));
    }

    @Test
    public void whenHexValueIsTooShort_thenViolation() {
        ColorForm form = new ColorForm();
        form.setHexValue("FFF"); // Too short
        form.setPosition(1);

        Set<ConstraintViolation<ColorForm>> violations = validator.validate(form);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Invalid hexadecimal format"));
    }

    @Test
    public void whenHexValueIsBlank_thenViolation() {
        ColorForm form = new ColorForm();
        form.setHexValue("");
        form.setPosition(1);

        Set<ConstraintViolation<ColorForm>> violations = validator.validate(form);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Hexadecimal value is required"));
    }
}
