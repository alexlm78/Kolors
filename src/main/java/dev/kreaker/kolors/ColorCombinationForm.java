package dev.kreaker.kolors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ColorCombinationForm {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @Valid
    @NotEmpty(message = "Must have at least one color")
    private List<ColorForm> colors = new ArrayList<>();

    // Constructors
    public ColorCombinationForm() {
        // Initialize with one empty color by default
        colors.add(new ColorForm("", 1));
    }

    public ColorCombinationForm(String name) {
        this();
        this.name = name;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Dynamic color count calculation
    public Integer getColorCount() {
        return colors != null ? colors.size() : 0;
    }

    public List<ColorForm> getColors() {
        return colors;
    }

    public void setColors(List<ColorForm> colors) {
        this.colors = colors != null ? colors : new ArrayList<>();
        // Ensure positions are sequential
        reorderPositions();
    }

    // Dynamic color management methods
    public void addColor() {
        if (colors == null) {
            colors = new ArrayList<>();
        }
        int nextPosition = getNextAvailablePosition();
        colors.add(new ColorForm("", nextPosition));
    }

    public void addColor(String hexValue) {
        if (colors == null) {
            colors = new ArrayList<>();
        }
        int nextPosition = getNextAvailablePosition();
        colors.add(new ColorForm(hexValue, nextPosition));
    }

    public void removeColor(int index) {
        if (colors != null && index >= 0 && index < colors.size() && colors.size() > 1) {
            colors.remove(index);
            reorderPositions();
        }
    }

    public void removeColorAtPosition(Integer position) {
        if (colors != null && colors.size() > 1) {
            colors.removeIf(color -> color.getPosition().equals(position));
            reorderPositions();
        }
    }

    private int getNextAvailablePosition() {
        return colors.stream().mapToInt(ColorForm::getPosition).max().orElse(0) + 1;
    }

    private void reorderPositions() {
        if (colors != null) {
            for (int i = 0; i < colors.size(); i++) {
                colors.get(i).setPosition(i + 1);
            }
        }
    }

    // Validation methods
    public boolean isValid() {
        if (name == null || name.trim().length() < 3) {
            return false;
        }
        if (colors == null || colors.isEmpty()) {
            return false;
        }
        return colors.stream().allMatch(ColorForm::isValidHex);
    }

    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();

        if (name == null || name.trim().length() < 3) {
            errors.add("Name must have at least 3 characters");
        }
        if (colors == null || colors.isEmpty()) {
            errors.add("Must have at least one color");
        } else {
            for (int i = 0; i < colors.size(); i++) {
                ColorForm color = colors.get(i);
                if (!color.isValidHex()) {
                    errors.add("Color " + (i + 1) + " has invalid hexadecimal format");
                }
            }
        }

        return errors;
    }

    // Convert to entity
    public ColorCombination toEntity() {
        ColorCombination combination = new ColorCombination(this.name, this.getColorCount());

        if (colors != null && !colors.isEmpty()) {
            List<ColorInCombination> colorEntities =
                    colors.stream().map(ColorForm::toEntity).collect(Collectors.toList());
            combination.setColors(colorEntities);
        }

        return combination;
    }

    // Create from entity
    public static ColorCombinationForm fromEntity(ColorCombination combination) {
        if (combination == null) {
            return null;
        }

        ColorCombinationForm form = new ColorCombinationForm();
        form.setName(combination.getName());

        if (combination.getColors() != null && !combination.getColors().isEmpty()) {
            List<ColorForm> colorForms =
                    combination.getColors().stream()
                            .sorted((c1, c2) -> c1.getPosition().compareTo(c2.getPosition()))
                            .map(ColorForm::fromEntity)
                            .collect(Collectors.toList());
            form.setColors(colorForms);
        } else {
            // Ensure at least one color field
            form.colors.clear();
            form.colors.add(new ColorForm("", 1));
        }

        return form;
    }

    @Override
    public String toString() {
        return "ColorCombinationForm{"
                + "name='"
                + name
                + '\''
                + ", colorCount="
                + getColorCount()
                + ", colors="
                + colors
                + '}';
    }
}
