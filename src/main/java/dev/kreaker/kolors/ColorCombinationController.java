package dev.kreaker.kolors;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.service.ColorCombinationService;
import jakarta.validation.Valid;

/**
 * Controlador principal para gestión de combinaciones de colores
 * Maneja todas las operaciones CRUD y búsqueda de combinaciones
 */
@Controller
@RequestMapping("/combinations")
public class ColorCombinationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ColorCombinationController.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    
    private final ColorCombinationService colorCombinationService;
    
    public ColorCombinationController(ColorCombinationService colorCombinationService) {
        this.colorCombinationService = colorCombinationService;
    }
    
    /**
     * Página principal - lista todas las combinaciones con búsqueda y filtrado
     */
    @GetMapping({"", "/"})
    public String index(Model model, 
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) Integer colorCount,
                       @RequestParam(required = false) String hexValue,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size) {
        
        logger.debug("Accediendo a página principal - búsqueda: '{}', colores: {}, hex: '{}', página: {}", 
                    search, colorCount, hexValue, page);
        
        try {
            List<ColorCombination> combinations;
            
            // Aplicar filtros de búsqueda
            if (hexValue != null && !hexValue.trim().isEmpty()) {
                combinations = colorCombinationService.findByHexValue(hexValue.trim());
            } else {
                combinations = colorCombinationService.searchCombinations(search, colorCount);
            }
            
            // Agregar datos al modelo
            model.addAttribute("combinations", combinations);
            model.addAttribute("search", search);
            model.addAttribute("colorCount", colorCount);
            model.addAttribute("hexValue", hexValue);
            model.addAttribute("totalCombinations", combinations.size());
            
            // Agregar formulario vacío para crear nueva combinación
            if (!model.containsAttribute("combinationForm")) {
                model.addAttribute("combinationForm", new ColorCombinationForm());
            }
            
            return "combinations/index";
            
        } catch (Exception e) {
            logger.error("Error al cargar página principal", e);
            model.addAttribute("error", "Error al cargar las combinaciones de colores");
            model.addAttribute("combinations", List.of());
            model.addAttribute("combinationForm", new ColorCombinationForm());
            return "combinations/index";
        }
    }
    
    /**
     * Crear nueva combinación de colores
     */
    @PostMapping("/create")
    public String createCombination(@Valid @ModelAttribute("combinationForm") ColorCombinationForm form,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        
        logger.info("Intentando crear nueva combinación: {}", form.getName());
        
        try {
            // Si hay errores de validación, volver al formulario
            if (result.hasErrors()) {
                logger.warn("Errores de validación al crear combinación: {}", result.getAllErrors());
                return addFormDataAndReturnIndex(model, form, "Errores en el formulario. Por favor, corrija los datos.");
            }
            
            // Crear la combinación
            ColorCombination savedCombination = colorCombinationService.createCombination(form);
            
            // Mensaje de éxito
            redirectAttributes.addFlashAttribute("success", 
                "Combinación '" + savedCombination.getName() + "' creada exitosamente");
            
            logger.info("Combinación creada exitosamente: {} (ID: {})", 
                       savedCombination.getName(), savedCombination.getId());
            
            return "redirect:/combinations/";
            
        } catch (ColorCombinationValidationException e) {
            logger.warn("Error de validación al crear combinación: {}", e.getMessage());
            model.addAttribute("error", "Errores de validación: " + String.join(", ", e.getValidationErrors()));
            return addFormDataAndReturnIndex(model, form, null);
            
        } catch (Exception e) {
            logger.error("Error inesperado al crear combinación", e);
            model.addAttribute("error", "Error inesperado al crear la combinación. Por favor, inténtelo de nuevo.");
            return addFormDataAndReturnIndex(model, form, null);
        }
    }
    
    /**
     * Mostrar formulario de edición
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        
        logger.debug("Mostrando formulario de edición para combinación ID: {}", id);
        
        try {
            ColorCombination combination = colorCombinationService.getById(id);
            ColorCombinationForm form = colorCombinationService.convertToForm(combination);
            
            model.addAttribute("combinationForm", form);
            model.addAttribute("combination", combination);
            model.addAttribute("isEditing", true);
            
            return "combinations/edit";
            
        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combinación no encontrada para edición: {}", id);
            redirectAttributes.addFlashAttribute("error", "Combinación no encontrada");
            return "redirect:/combinations/";
            
        } catch (Exception e) {
            logger.error("Error al cargar formulario de edición para ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Error al cargar la combinación para edición");
            return "redirect:/combinations/";
        }
    }
    
    /**
     * Actualizar combinación existente
     */
    @PostMapping("/{id}/update")
    public String updateCombination(@PathVariable Long id,
                                   @Valid @ModelAttribute("combinationForm") ColorCombinationForm form,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        
        logger.info("Intentando actualizar combinación ID: {} con nombre: {}", id, form.getName());
        
        try {
            // Si hay errores de validación, volver al formulario de edición
            if (result.hasErrors()) {
                logger.warn("Errores de validación al actualizar combinación ID {}: {}", id, result.getAllErrors());
                ColorCombination combination = colorCombinationService.getById(id);
                model.addAttribute("combination", combination);
                model.addAttribute("isEditing", true);
                model.addAttribute("error", "Errores en el formulario. Por favor, corrija los datos.");
                return "combinations/edit";
            }
            
            // Actualizar la combinación
            ColorCombination updatedCombination = colorCombinationService.updateCombination(id, form);
            
            // Mensaje de éxito
            redirectAttributes.addFlashAttribute("success", 
                "Combinación '" + updatedCombination.getName() + "' actualizada exitosamente");
            
            logger.info("Combinación actualizada exitosamente: {} (ID: {})", 
                       updatedCombination.getName(), updatedCombination.getId());
            
            return "redirect:/combinations/";
            
        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combinación no encontrada para actualización: {}", id);
            redirectAttributes.addFlashAttribute("error", "Combinación no encontrada");
            return "redirect:/combinations/";
            
        } catch (ColorCombinationValidationException e) {
            logger.warn("Error de validación al actualizar combinación ID {}: {}", id, e.getMessage());
            try {
                ColorCombination combination = colorCombinationService.getById(id);
                model.addAttribute("combination", combination);
                model.addAttribute("isEditing", true);
                model.addAttribute("error", "Errores de validación: " + String.join(", ", e.getValidationErrors()));
                return "combinations/edit";
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("error", "Error de validación: " + e.getMessage());
                return "redirect:/combinations/";
            }
            
        } catch (Exception e) {
            logger.error("Error inesperado al actualizar combinación ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Error inesperado al actualizar la combinación");
            return "redirect:/combinations/";
        }
    }
    
    /**
     * Mostrar página de confirmación de eliminación
     */
    @GetMapping("/{id}/confirm-delete")
    public String confirmDelete(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        
        logger.debug("Mostrando confirmación de eliminación para combinación ID: {}", id);
        
        try {
            ColorCombination combination = colorCombinationService.getById(id);
            model.addAttribute("combination", combination);
            return "combinations/confirm-delete";
            
        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combinación no encontrada para eliminación: {}", id);
            redirectAttributes.addFlashAttribute("error", "Combinación no encontrada");
            return "redirect:/combinations/";
            
        } catch (Exception e) {
            logger.error("Error al cargar confirmación de eliminación para ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Error al cargar la combinación");
            return "redirect:/combinations/";
        }
    }
    
    /**
     * Eliminar combinación de colores
     */
    @PostMapping("/{id}/delete")
    public String deleteCombination(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        
        logger.info("Intentando eliminar combinación ID: {}", id);
        
        try {
            // Obtener nombre antes de eliminar para el mensaje
            ColorCombination combination = colorCombinationService.getById(id);
            String combinationName = combination.getName();
            
            // Eliminar la combinación
            colorCombinationService.deleteCombination(id);
            
            // Mensaje de éxito
            redirectAttributes.addFlashAttribute("success", 
                "Combinación '" + combinationName + "' eliminada exitosamente");
            
            logger.info("Combinación eliminada exitosamente: {} (ID: {})", combinationName, id);
            
        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combinación no encontrada para eliminación: {}", id);
            redirectAttributes.addFlashAttribute("error", "Combinación no encontrada");
            
        } catch (Exception e) {
            logger.error("Error inesperado al eliminar combinación ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la combinación. Por favor, inténtelo de nuevo.");
        }
        
        return "redirect:/combinations/";
    }
    
    /**
     * Ver detalles de una combinación específica
     */
    @GetMapping("/{id}")
    public String viewCombination(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        
        logger.debug("Mostrando detalles de combinación ID: {}", id);
        
        try {
            ColorCombination combination = colorCombinationService.getById(id);
            model.addAttribute("combination", combination);
            return "combinations/view";
            
        } catch (ColorCombinationNotFoundException e) {
            logger.warn("Combinación no encontrada para visualización: {}", id);
            redirectAttributes.addFlashAttribute("error", "Combinación no encontrada");
            return "redirect:/combinations/";
            
        } catch (Exception e) {
            logger.error("Error al cargar detalles de combinación ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Error al cargar los detalles de la combinación");
            return "redirect:/combinations/";
        }
    }
    
    /**
     * API endpoint para búsqueda AJAX (opcional)
     */
    @GetMapping("/search")
    public String searchCombinations(@RequestParam(required = false) String term,
                                   @RequestParam(required = false) Integer colorCount,
                                   @RequestParam(required = false) String hexValue,
                                   Model model) {
        
        logger.debug("Búsqueda AJAX - término: '{}', colores: {}, hex: '{}'", term, colorCount, hexValue);
        
        try {
            List<ColorCombination> combinations;
            
            if (hexValue != null && !hexValue.trim().isEmpty()) {
                combinations = colorCombinationService.findByHexValue(hexValue.trim());
            } else {
                combinations = colorCombinationService.searchCombinations(term, colorCount);
            }
            
            model.addAttribute("combinations", combinations);
            return "combinations/fragments/combination-list :: combinationList";
            
        } catch (Exception e) {
            logger.error("Error en búsqueda AJAX", e);
            model.addAttribute("combinations", List.of());
            model.addAttribute("error", "Error en la búsqueda");
            return "combinations/fragments/combination-list :: combinationList";
        }
    }
    
    /**
     * Método auxiliar para agregar datos del formulario y retornar a la página principal
     */
    private String addFormDataAndReturnIndex(Model model, ColorCombinationForm form, String errorMessage) {
        try {
            // Agregar combinaciones existentes
            List<ColorCombination> combinations = colorCombinationService.findAllCombinations();
            model.addAttribute("combinations", combinations);
            
            // Agregar formulario con datos
            model.addAttribute("combinationForm", form);
            model.addAttribute("totalCombinations", combinations.size());
            
            // Agregar mensaje de error si se proporciona
            if (errorMessage != null) {
                model.addAttribute("error", errorMessage);
            }
            
            return "combinations/index";
            
        } catch (Exception e) {
            logger.error("Error al cargar datos para formulario", e);
            model.addAttribute("combinations", List.of());
            model.addAttribute("combinationForm", form != null ? form : new ColorCombinationForm());
            model.addAttribute("error", "Error al cargar los datos");
            return "combinations/index";
        }
    }
    
    /**
     * Manejo de errores específico del controlador
     */
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        // Agregar atributos comunes que siempre están disponibles
        model.addAttribute("pageTitle", "Gestión de Combinaciones de Colores");
        model.addAttribute("colorCountOptions", List.of(2, 3, 4));
    }
}