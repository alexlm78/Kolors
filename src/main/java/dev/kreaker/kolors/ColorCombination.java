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

    @NotNull(message = "Must have at least one color")
    @Min(value = 1, message = "Minimum 1 color")
    @Column(nullable = false, name = "color_count")
    private Integer colorCount;

    // Constructors
    public ColorCombination() {
        this.createdAt = LocalDateTime.now();
    }

    public ColorCombination(String name, Integer colorCount) {
        this();
        this.name = name;
        this.colorCount = colorCount != null ? Math.max(colorCount, 1) : 1;
    }

    public ColorCombination(String name) {
        this();
        this.name = name;
        this.colorCount = 1; // Default to 1 color, will be updated when colors are added
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
        // Ensure bidirectional relationship and update color count
        if (colors != null) {
            colors.forEach(color -> color.setCombination(this));
            this.colorCount = colors.size();
        } else {
            this.colorCount = 0;
        }
    }

    public Integer getColorCount() {
        return colorCount;
    }

    public void setColorCount(Integer colorCount) {
        this.colorCount = colorCount;
    }

    // Helper methods for dynamic color management
    public void addColor(ColorInCombination color) {
        if (color != null) {
            colors.add(color);
            color.setCombination(this);
            updateColorCount();
        }
    }

    public void removeColor(ColorInCombination color) {
        if (color != null && colors.remove(color)) {
            color.setCombination(null);
            updateColorCount();
            reorderPositions();
        }
    }

    public void addColorAtPosition(String hexValue, Integer position) {
        ColorInCombination color = new ColorInCombination(hexValue, position, this);
        colors.add(color);
        updateColorCount();
    }

    public void removeColorAtPosition(Integer position) {
        colors.removeIf(color -> color.getPosition().equals(position));
        updateColorCount();
        reorderPositions();
    }

    private void updateColorCount() {
        this.colorCount = colors.size();
    }

    private void reorderPositions() {
        // Reorder positions to be sequential (1, 2, 3, ...)
        colors.sort((c1, c2) -> c1.getPosition().compareTo(c2.getPosition()));
        for (int i = 0; i < colors.size(); i++) {
            colors.get(i).setPosition(i + 1);
        }
    }

    public Integer getNextAvailablePosition() {
        return colors.stream()
                .mapToInt(ColorInCombination::getPosition)
                .max()
                .orElse(0) + 1;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "ColorCombination{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", colorCount=" + colorCount
                + ", createdAt=" + createdAt
                + '}';
    }
}
