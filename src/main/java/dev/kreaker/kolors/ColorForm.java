package dev.kreaker.kolors;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class ColorForm {
    
    @NotBlank(message = "Hexadecimal value is required")
    @Pattern(regexp = "^[0-9A-Fa-f]{6}$", message = "Invalid hexadecimal format: must be 6 characters (0-9, A-F)")
    private String hexValue;
    
    @NotNull(message = "La posición es obligatoria")
    @Min(value = 1, message = "La posición mínima es 1")
    @Max(value = 4, message = "La posición máxima es 4")
    private Integer position;
    
    // Constructors
    public ColorForm() {
    }
    
    public ColorForm(String hexValue, Integer position) {
        this.hexValue = hexValue;
        this.position = position;
    }
    
    // Getters and Setters
    public String getHexValue() {
        return hexValue;
    }
    
    public void setHexValue(String hexValue) {
        this.hexValue = hexValue;
    }
    
    public Integer getPosition() {
        return position;
    }
    
    public void setPosition(Integer position) {
        this.position = position;
    }
    
    // Helper methods
    public String getFormattedHex() {
        return hexValue != null ? "#" + hexValue : null;
    }
    
    public boolean isValidHex() {
        return hexValue != null && hexValue.matches("^[0-9A-Fa-f]{6}$");
    }
    
    // Convert to entity
    public ColorInCombination toEntity() {
        return new ColorInCombination(this.hexValue, this.position);
    }
    
    // Create from entity
    public static ColorForm fromEntity(ColorInCombination colorInCombination) {
        if (colorInCombination == null) {
            return null;
        }
        return new ColorForm(colorInCombination.getHexValue(), colorInCombination.getPosition());
    }
    
    @Override
    public String toString() {
        return "ColorForm{" +
                "hexValue='" + hexValue + '\'' +
                ", position=" + position +
                '}';
    }
}