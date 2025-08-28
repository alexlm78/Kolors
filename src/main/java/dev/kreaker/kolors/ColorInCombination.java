package dev.kreaker.kolors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "color_in_combination", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"combination_id", "position"}))
public class ColorInCombination {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El valor hexadecimal es obligatorio")
    @Pattern(regexp = "^[0-9A-Fa-f]{6}$", message = "Formato hexadecimal inválido: debe ser 6 caracteres (0-9, A-F)")
    @Column(nullable = false, length = 6, name = "hex_value")
    private String hexValue;
    
    @NotNull(message = "La posición es obligatoria")
    @Min(value = 1, message = "La posición mínima es 1")
    @Max(value = 4, message = "La posición máxima es 4")
    @Column(nullable = false)
    private Integer position;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combination_id", nullable = false)
    private ColorCombination combination;
    
    // Constructors
    public ColorInCombination() {
    }
    
    public ColorInCombination(String hexValue, Integer position) {
        this.hexValue = hexValue;
        this.position = position;
    }
    
    public ColorInCombination(String hexValue, Integer position, ColorCombination combination) {
        this.hexValue = hexValue;
        this.position = position;
        this.combination = combination;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public ColorCombination getCombination() {
        return combination;
    }
    
    public void setCombination(ColorCombination combination) {
        this.combination = combination;
    }
    
    // Helper methods
    public String getFormattedHex() {
        return "#" + hexValue;
    }
    
    public boolean isValidHex() {
        return hexValue != null && hexValue.matches("^[0-9A-Fa-f]{6}$");
    }
    
    @Override
    public String toString() {
        return "ColorInCombination{" +
                "id=" + id +
                ", hexValue='" + hexValue + '\'' +
                ", position=" + position +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColorInCombination)) return false;
        
        ColorInCombination that = (ColorInCombination) o;
        
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }
        
        // Both hexValue and position must match for equality
        boolean hexEquals = hexValue != null ? hexValue.equals(that.hexValue) : that.hexValue == null;
        boolean positionEquals = position != null ? position.equals(that.position) : that.position == null;
        
        return hexEquals && positionEquals;
    }
    
    @Override
    public int hashCode() {
        int result = hexValue != null ? hexValue.hashCode() : 0;
        result = 31 * result + (position != null ? position.hashCode() : 0);
        return result;
    }
}