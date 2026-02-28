package dev.kreaker.kolors;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kreaker.kolors.dto.ColorCombinationForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ColorCombinationFormTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void whenNameIsValid_thenNoViolations() {
        ColorCombinationForm form = new ColorCombinationForm();
        form.getColors().clear(); // Clear default invalid color
        form.setName("Valid Name");
        form.addColor("FF5733");

        Set<ConstraintViolation<ColorCombinationForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    public void whenNameIsBlank_thenViolation() {
        ColorCombinationForm form = new ColorCombinationForm();
        form.getColors().clear(); // Clear default invalid color
        form.setName("");
        form.addColor("FF5733");

        Set<ConstraintViolation<ColorCombinationForm>> violations = validator.validate(form);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Name is required") || v.getMessage().contains("must not be blank"));
    }

    @Test
    public void whenColorsAreEmpty_thenViolation() {
        ColorCombinationForm form = new ColorCombinationForm();
        form.setName("Valid Name");
        form.getColors().clear(); // Clear default colors

        Set<ConstraintViolation<ColorCombinationForm>> violations = validator.validate(form);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Must have at least one color"));
    }
}
