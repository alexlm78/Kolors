package dev.kreaker.kolors.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.kreaker.kolors.ColorCombination;
import dev.kreaker.kolors.ColorCombinationForm;
import dev.kreaker.kolors.ColorCombinationRepository;
import dev.kreaker.kolors.ColorForm;
import dev.kreaker.kolors.ColorInCombination;
import dev.kreaker.kolors.ColorInCombinationRepository;
import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.exception.InvalidColorFormatException;

/**
 * Servicio de negocio para gestión de combinaciones de colores
 * Proporciona operaciones CRUD y validación de datos
 */
@Service
@Transactional
public class ColorCombinationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ColorCombinationService.class);
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^[0-9A-Fa-f]{6}$");
    
    private final ColorCombinationRepository colorCombinationRepository;
    private final ColorInCombinationRepository colorInCombinationRepository;
    
    @Autowired
    public ColorCombinationService(ColorCombinationRepository colorCombinationRepository,
                                 ColorInCombinationRepository colorInCombinationRepository) {
        this.colorCombinationRepository = colorCombinationRepository;
        this.colorInCombinationRepository = colorInCombinationRepository;
    }
    
    /**
     * Crea una nueva combinación de colores
     */
    public ColorCombination createCombination(ColorCombinationForm form) {
        logger.info("Creando nueva combinación de colores: {}", form.getName());
        
        // Validar el formulario
        validateForm(form);
        
        // Crear la entidad
        ColorCombination combination = new ColorCombination(form.getName(), form.getColorCount());
        
        // Agregar colores
        if (form.getColors() != null) {
            for (ColorForm colorForm : form.getColors()) {
                ColorInCombination color = new ColorInCombination(
                    colorForm.getHexValue().toUpperCase(), 
                    colorForm.getPosition()
                );
                combination.addColor(color);
            }
        }
        
        // Guardar en base de datos
        ColorCombination savedCombination = colorCombinationRepository.save(combination);
        logger.info("Combinación creada exitosamente con ID: {}", savedCombination.getId());
        
        return savedCombination;
    }
    
    /**
     * Obtiene todas las combinaciones de colores
     */
    @Transactional(readOnly = true)
    public List<ColorCombination> findAllCombinations() {
        logger.debug("Obteniendo todas las combinaciones de colores");
        return colorCombinationRepository.findAllByOrderByCreatedAtDesc();
    }
    
    /**
     * Obtiene todas las combinaciones con paginación
     */
    @Transactional(readOnly = true)
    public Page<ColorCombination> findAllCombinations(Pageable pageable) {
        logger.debug("Obteniendo combinaciones con paginación: {}", pageable);
        return colorCombinationRepository.findAllByOrderByNameAsc(pageable);
    }
    
    /**
     * Busca combinaciones por criterios específicos
     */
    @Transactional(readOnly = true)
    public List<ColorCombination> searchCombinations(String searchTerm, Integer colorCount) {
        logger.debug("Buscando combinaciones - término: '{}', número de colores: {}", searchTerm, colorCount);
        
        if (searchTerm != null && !searchTerm.trim().isEmpty() && colorCount != null) {
            return colorCombinationRepository.findSimilarCombinations(searchTerm.trim(), colorCount);
        } else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return colorCombinationRepository.findByNameContainingIgnoreCase(searchTerm.trim());
        } else if (colorCount != null) {
            return colorCombinationRepository.findByColorCount(colorCount);
        } else {
            return findAllCombinations();
        }
    }
    
    /**
     * Busca combinaciones que contengan un color específico
     */
    @Transactional(readOnly = true)
    public List<ColorCombination> findByHexValue(String hexValue) {
        logger.debug("Buscando combinaciones que contengan el color: {}", hexValue);
        
        if (!isValidHexColor(hexValue)) {
            throw InvalidColorFormatException.forHexValue(hexValue);
        }
        
        return colorCombinationRepository.findByContainingHexValue(hexValue.toUpperCase());
    }
    
    /**
     * Obtiene una combinación por ID
     */
    @Transactional(readOnly = true)
    public Optional<ColorCombination> findById(Long id) {
        logger.debug("Buscando combinación por ID: {}", id);
        return colorCombinationRepository.findById(id);
    }
    
    /**
     * Obtiene una combinación por ID o lanza excepción si no existe
     */
    @Transactional(readOnly = true)
    public ColorCombination getById(Long id) {
        logger.debug("Obteniendo combinación por ID: {}", id);
        return colorCombinationRepository.findById(id)
                .orElseThrow(() -> new ColorCombinationNotFoundException(id));
    }
    
    /**
     * Actualiza una combinación existente
     */
    @Transactional
    public ColorCombination updateCombination(Long id, ColorCombinationForm form) {
        logger.info("Actualizando combinación ID: {} con datos: {}", id, form.getName());
        
        // Validar el formulario
        validateForm(form);
        
        // Obtener la combinación existente
        ColorCombination existingCombination = getById(id);
        
        // Actualizar datos básicos
        existingCombination.setName(form.getName());
        existingCombination.setColorCount(form.getColorCount());
        
        // Eliminar colores existentes de la base de datos primero
        colorInCombinationRepository.deleteByCombinationId(existingCombination.getId());
        
        // Limpiar la colección en memoria
        existingCombination.getColors().clear();
        
        // Agregar nuevos colores
        if (form.getColors() != null) {
            for (ColorForm colorForm : form.getColors()) {
                ColorInCombination color = new ColorInCombination(
                    colorForm.getHexValue().toUpperCase(), 
                    colorForm.getPosition()
                );
                existingCombination.addColor(color);
            }
        }
        
        // Guardar cambios
        ColorCombination updatedCombination = colorCombinationRepository.save(existingCombination);
        logger.info("Combinación actualizada exitosamente: {}", updatedCombination.getId());
        
        return updatedCombination;
    }
    
    /**
     * Elimina una combinación de colores
     */
    public void deleteCombination(Long id) {
        logger.info("Eliminando combinación ID: {}", id);
        
        // Verificar que existe
        if (!colorCombinationRepository.existsById(id)) {
            throw new ColorCombinationNotFoundException(id);
        }
        
        // Eliminar (los colores se eliminan automáticamente por cascade)
        colorCombinationRepository.deleteById(id);
        logger.info("Combinación eliminada exitosamente: {}", id);
    }
    
    /**
     * Valida si un color hexadecimal es válido
     */
    public boolean isValidHexColor(String hexValue) {
        return hexValue != null && HEX_COLOR_PATTERN.matcher(hexValue).matches();
    }
    
    /**
     * Valida una lista de colores hexadecimales
     */
    public boolean validateHexColors(List<String> hexValues) {
        if (hexValues == null || hexValues.isEmpty()) {
            return false;
        }
        
        return hexValues.stream().allMatch(this::isValidHexColor);
    }
    
    /**
     * Valida que el número de colores coincida con la lista de colores
     */
    public boolean validateColorCount(Integer colorCount, List<ColorForm> colors) {
        if (colorCount == null || colors == null) {
            return false;
        }
        
        if (colorCount < 2 || colorCount > 4) {
            return false;
        }
        
        return colors.size() == colorCount;
    }
    
    /**
     * Convierte una entidad a formulario
     */
    @Transactional(readOnly = true)
    public ColorCombinationForm convertToForm(ColorCombination combination) {
        if (combination == null) {
            return null;
        }
        
        return ColorCombinationForm.fromEntity(combination);
    }
    
    /**
     * Verifica si existe una combinación con el mismo nombre
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return colorCombinationRepository.existsByNameIgnoreCase(name);
    }
    
    /**
     * Obtiene estadísticas de combinaciones
     */
    @Transactional(readOnly = true)
    public CombinationStatistics getStatistics() {
        logger.debug("Obteniendo estadísticas de combinaciones");
        
        long totalCombinations = colorCombinationRepository.count();
        long combinationsWith2Colors = colorCombinationRepository.countByColorCount(2);
        long combinationsWith3Colors = colorCombinationRepository.countByColorCount(3);
        long combinationsWith4Colors = colorCombinationRepository.countByColorCount(4);
        
        return new CombinationStatistics(
            totalCombinations,
            combinationsWith2Colors,
            combinationsWith3Colors,
            combinationsWith4Colors
        );
    }
    
    /**
     * Obtiene las combinaciones más recientes
     */
    @Transactional(readOnly = true)
    public List<ColorCombination> getRecentCombinations(int limit) {
        logger.debug("Obteniendo {} combinaciones más recientes", limit);
        return colorCombinationRepository.findMostRecent(
            org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }
    
    /**
     * Valida un formulario de combinación completo
     */
    private void validateForm(ColorCombinationForm form) {
        List<String> errors = new ArrayList<>();
        
        // Validar nombre
        if (form.getName() == null || form.getName().trim().length() < 3) {
            errors.add("El nombre debe tener al menos 3 caracteres");
        }
        
        // Validar número de colores
        if (form.getColorCount() == null || form.getColorCount() < 2 || form.getColorCount() > 4) {
            errors.add("El número de colores debe ser entre 2 y 4");
        }
        
        // Validar lista de colores
        if (form.getColors() == null || form.getColors().isEmpty()) {
            errors.add("Debe proporcionar al menos un color");
        } else {
            // Validar que el número de colores coincida PRIMERO
            if (form.getColorCount() != null && form.getColors().size() != form.getColorCount()) {
                errors.add("El número de colores proporcionados (" + form.getColors().size() + 
                          ") no coincide con el especificado (" + form.getColorCount() + ")");
            } else {
                // Solo validar formato de colores si el número coincide
                for (int i = 0; i < form.getColors().size(); i++) {
                    ColorForm color = form.getColors().get(i);
                    if (color.getHexValue() == null || !isValidHexColor(color.getHexValue())) {
                        errors.add("Color en posición " + (i + 1) + " tiene formato hexadecimal inválido");
                    }
                    if (color.getPosition() == null || color.getPosition() < 1 || color.getPosition() > 4) {
                        errors.add("Color en posición " + (i + 1) + " tiene posición inválida");
                    }
                }
                
                // Validar posiciones únicas solo si el número coincide
                List<Integer> positions = form.getColors().stream()
                        .map(ColorForm::getPosition)
                        .filter(pos -> pos != null)
                        .collect(Collectors.toList());
                
                if (positions.size() != positions.stream().distinct().count()) {
                    errors.add("Las posiciones de los colores deben ser únicas");
                }
            }
        }
        
        // Lanzar excepción si hay errores
        if (!errors.isEmpty()) {
            throw new ColorCombinationValidationException(errors);
        }
    }
    
    /**
     * Clase interna para estadísticas
     */
    public static class CombinationStatistics {
        private final long totalCombinations;
        private final long combinationsWith2Colors;
        private final long combinationsWith3Colors;
        private final long combinationsWith4Colors;
        
        public CombinationStatistics(long totalCombinations, long combinationsWith2Colors, 
                                   long combinationsWith3Colors, long combinationsWith4Colors) {
            this.totalCombinations = totalCombinations;
            this.combinationsWith2Colors = combinationsWith2Colors;
            this.combinationsWith3Colors = combinationsWith3Colors;
            this.combinationsWith4Colors = combinationsWith4Colors;
        }
        
        public long getTotalCombinations() { return totalCombinations; }
        public long getCombinationsWith2Colors() { return combinationsWith2Colors; }
        public long getCombinationsWith3Colors() { return combinationsWith3Colors; }
        public long getCombinationsWith4Colors() { return combinationsWith4Colors; }
    }
}