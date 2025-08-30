package dev.kreaker.kolors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "color_combination")
public class ColorCombination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "combination", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ColorInCombination> colors = new ArrayList<>();

    @NotNull(message = "Must specify the number of colors")
    @Min(value = 2, message = "Minimum 2 colors")
    @Max(value = 4, message = "Maximum 4 colors")
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