package dev.kreaker.kolors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ColorCombinationForm {
    
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;
    
    @NotNull(message = "Must specify the number of colors")
    @Min(value = 2, message = "Minimum 2 colors")
    @Max(value = 4, message = "Maximum 4 colors")
    private Integer colorCount;
    
    @Valid
    private List<ColorForm> colors = new ArrayList<>();
    
    // Constructors
    public ColorCombinationForm() {
    }
    
    public ColorCombinationForm(String name, Integer colorCount) {
        this.name = name;
        this.colorCount = colorCount;
        initializeColors();
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getColorCount() {
        return colorCount;
    }
    
    public void setColorCount(Integer colorCount) {
        this.colorCount = colorCount;
        initializeColors();
    }
    
    public List<ColorForm> getColors() {
        return colors;
    }
    
    public void setColors(List<ColorForm> colors) {
        this.colors = colors != null ? colors : new ArrayList<>();
    }
    
    // Helper methods
    private void initializeColors() {
        if (colorCount != null) {
            colors = new ArrayList<>();
            for (int i = 1; i <= colorCount; i++) {
                colors.add(new ColorForm("", i));
            }
        }
    }
    
    public boolean isValid() {
        if (name == null || name.trim().length() < 3) {
            return false;
        }
        if (colorCount == null || colorCount < 2 || colorCount > 4) {
            return false;
        }
        if (colors == null || colors.size() != colorCount) {
            return false;
        }
        return colors.stream().allMatch(ColorForm::isValidHex);
    }
    
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();
        
        if (name == null || name.trim().length() < 3) {
            errors.add("Name must have at least 3 characters");
        }
        if (colorCount == null || colorCount < 2 || colorCount > 4) {
            errors.add("Must specify between 2 and 4 colors");
        }
        if (colors == null || colors.size() != colorCount) {
            errors.add("Number of colors does not match specified count");
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
        ColorCombination combination = new ColorCombination(this.name, this.colorCount);
        
        if (colors != null) {
            List<ColorInCombination> colorEntities = colors.stream()
                    .map(ColorForm::toEntity)
                    .collect(Collectors.toList());
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
        form.setColorCount(combination.getColorCount());
        
        if (combination.getColors() != null) {
            List<ColorForm> colorForms = combination.getColors().stream()
                    .map(ColorForm::fromEntity)
                    .collect(Collectors.toList());
            form.setColors(colorForms);
        }
        
        return form;
    }
    
    @Override
    public String toString() {
        return "ColorCombinationForm{" +
                "name='" + name + '\'' +
                ", colorCount=" + colorCount +
                ", colors=" + colors +
                '}';
    }
}