package dev.kreaker.kolors;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "color_combination")
public class ColorCombination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    @Column(nullable = false, length = 100)
    private String name;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "combination", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ColorInCombination> colors = new ArrayList<>();

    @NotNull(message = "Debe especificar el número de colores")
    @Min(value = 2, message = "Mínimo 2 colores")
    @Max(value = 4, message = "Máximo 4 colores")
    @Column(nullable = false, name = "color_count")
    private Integer colorCount;

    // Constructors
    public ColorCombination() {
        this.createdAt = LocalDateTime.now();
    }

    public ColorCombination(String name, Integer colorCount) {
        this();
        this.name = name;
        this.colorCount = colorCount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ColorInCombination> getColors() {
        return colors;
    }

    public void setColors(List<ColorInCombination> colors) {
        this.colors = colors;
        // Ensure bidirectional relationship
        if (colors != null) {
            colors.forEach(color -> color.setCombination(this));
        }
    }

    public Integer getColorCount() {
        return colorCount;
    }

    public void setColorCount(Integer colorCount) {
        this.colorCount = colorCount;
    }

    // Helper methods
    public void addColor(ColorInCombination color) {
        colors.add(color);
        color.setCombination(this);
    }

    public void removeColor(ColorInCombination color) {
        colors.remove(color);
        color.setCombination(null);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "ColorCombination{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", colorCount=" + colorCount +
                ", createdAt=" + createdAt +
                '}';
    }
}