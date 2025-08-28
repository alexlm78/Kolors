package dev.kreaker.kolors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.service.ColorCombinationService;

/**
 * Tests de integración para ColorCombinationController
 * Enfocados en la lógica del controlador sin requerir templates
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.thymeleaf.enabled=false",
    "spring.main.web-application-type=servlet"
})
@DisplayName("ColorCombinationController Integration Tests")
class ColorCombinationControllerIntegrationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    private MockMvc mockMvc;
    
    @MockBean
    private ColorCombinationService colorCombinationService;
    
    private ColorCombination testCombination;
    private ColorCombinationForm testForm;
    private List<ColorCombination> testCombinations;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Crear combinación de prueba
        testCombination = new ColorCombination("Sunset Colors", 3);
        testCombination.setId(1L);
        testCombination.setCreatedAt(LocalDateTime.now());
        testCombination.addColor(new ColorInCombination("FF6B35", 1));
        testCombination.addColor(new ColorInCombination("F7931E", 2));
        testCombination.addColor(new ColorInCombination("FFD23F", 3));
        
        // Crear formulario de prueba
        testForm = new ColorCombinationForm("Ocean Breeze", 2);
        testForm.getColors().add(new ColorForm("0077BE", 1));
        testForm.getColors().add(new ColorForm("87CEEB", 2));
        
        // Lista de combinaciones de prueba
        testCombinations = Arrays.asList(testCombination);
        
        // Mock de estadísticas
        ColorCombinationService.CombinationStatistics stats = 
            new ColorCombinationService.CombinationStatistics(1, 0, 1, 0);
        when(colorCombinationService.getStatistics()).thenReturn(stats);
    }
    
    @Nested
    @DisplayName("Página Principal")
    class IndexPageTests {
        
        @Test
        @DisplayName("Debe responder correctamente a la página principal")
        void shouldRespondToIndexPage() throws Exception {
            // Given
            when(colorCombinationService.searchCombinations(null, null))
                .thenReturn(testCombinations);
            
            // When & Then
            mockMvc.perform(get("/combinations/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("combinations"))
                .andExpect(model().attributeExists("combinationForm"))
                .andExpect(model().attributeExists("statistics"));
            
            verify(colorCombinationService).searchCombinations(null, null);
            verify(colorCombinationService).getStatistics();
        }
        
        @Test
        @DisplayName("Debe manejar parámetros de búsqueda")
        void shouldHandleSearchParameters() throws Exception {
            // Given
            String searchTerm = "Sunset";
            when(colorCombinationService.searchCombinations(searchTerm, null))
                .thenReturn(testCombinations);
            
            // When & Then
            mockMvc.perform(get("/combinations/")
                    .param("search", searchTerm))
                .andExpect(status().isOk())
                .andExpect(model().attribute("search", searchTerm));
            
            verify(colorCombinationService).searchCombinations(searchTerm, null);
        }
        
        @Test
        @DisplayName("Debe manejar búsqueda por valor hexadecimal")
        void shouldHandleHexValueSearch() throws Exception {
            // Given
            String hexValue = "FF6B35";
            when(colorCombinationService.findByHexValue(hexValue))
                .thenReturn(testCombinations);
            
            // When & Then
            mockMvc.perform(get("/combinations/")
                    .param("hexValue", hexValue))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hexValue", hexValue));
            
            verify(colorCombinationService).findByHexValue(hexValue);
        }
    }
    
    @Nested
    @DisplayName("Crear Combinación")
    class CreateCombinationTests {
        
        @Test
        @DisplayName("Debe crear combinación válida exitosamente")
        void shouldCreateValidCombinationSuccessfully() throws Exception {
            // Given
            when(colorCombinationService.createCombination(any(ColorCombinationForm.class)))
                .thenReturn(testCombination);
            
            // When & Then
            mockMvc.perform(post("/combinations/create")
                    .param("name", "Ocean Breeze")
                    .param("colorCount", "2")
                    .param("colors[0].hexValue", "0077BE")
                    .param("colors[0].position", "1")
                    .param("colors[1].hexValue", "87CEEB")
                    .param("colors[1].position", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/combinations/"))
                .andExpect(flash().attributeExists("success"));
            
            verify(colorCombinationService).createCombination(any(ColorCombinationForm.class));
        }
        
        @Test
        @DisplayName("Debe manejar errores de validación del servicio")
        void shouldHandleServiceValidationErrors() throws Exception {
            // Given
            List<String> validationErrors = Arrays.asList("Error 1", "Error 2");
            when(colorCombinationService.createCombination(any(ColorCombinationForm.class)))
                .thenThrow(new ColorCombinationValidationException(validationErrors));
            when(colorCombinationService.findAllCombinations()).thenReturn(testCombinations);
            
            // When & Then
            mockMvc.perform(post("/combinations/create")
                    .param("name", "Test Combination")
                    .param("colorCount", "2")
                    .param("colors[0].hexValue", "0077BE")
                    .param("colors[0].position", "1")
                    .param("colors[1].hexValue", "87CEEB")
                    .param("colors[1].position", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
        }
    }
    
    @Nested
    @DisplayName("Editar Combinación")
    class EditCombinationTests {
        
        @Test
        @DisplayName("Debe mostrar formulario de edición")
        void shouldDisplayEditForm() throws Exception {
            // Given
            when(colorCombinationService.getById(1L)).thenReturn(testCombination);
            when(colorCombinationService.convertToForm(testCombination)).thenReturn(testForm);
            
            // When & Then
            mockMvc.perform(get("/combinations/1/edit"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("combinationForm"))
                .andExpect(model().attributeExists("combination"))
                .andExpect(model().attribute("isEditing", true));
            
            verify(colorCombinationService).getById(1L);
            verify(colorCombinationService).convertToForm(testCombination);
        }
        
        @Test
        @DisplayName("Debe manejar combinación no encontrada en edición")
        void shouldHandleNotFoundInEdit() throws Exception {
            // Given
            when(colorCombinationService.getById(999L))
                .thenThrow(new ColorCombinationNotFoundException(999L));
            
            // When & Then
            mockMvc.perform(get("/combinations/999/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/combinations/"))
                .andExpect(flash().attributeExists("error"));
        }
        
        @Test
        @DisplayName("Debe actualizar combinación exitosamente")
        void shouldUpdateCombinationSuccessfully() throws Exception {
            // Given
            when(colorCombinationService.updateCombination(eq(1L), any(ColorCombinationForm.class)))
                .thenReturn(testCombination);
            
            // When & Then
            mockMvc.perform(post("/combinations/1/update")
                    .param("name", "Updated Ocean Breeze")
                    .param("colorCount", "2")
                    .param("colors[0].hexValue", "0077BE")
                    .param("colors[0].position", "1")
                    .param("colors[1].hexValue", "87CEEB")
                    .param("colors[1].position", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/combinations/"))
                .andExpect(flash().attributeExists("success"));
            
            verify(colorCombinationService).updateCombination(eq(1L), any(ColorCombinationForm.class));
        }
    }
    
    @Nested
    @DisplayName("Eliminar Combinación")
    class DeleteCombinationTests {
        
        @Test
        @DisplayName("Debe mostrar confirmación de eliminación")
        void shouldDisplayDeleteConfirmation() throws Exception {
            // Given
            when(colorCombinationService.getById(1L)).thenReturn(testCombination);
            
            // When & Then
            mockMvc.perform(get("/combinations/1/confirm-delete"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("combination"));
            
            verify(colorCombinationService).getById(1L);
        }
        
        @Test
        @DisplayName("Debe eliminar combinación exitosamente")
        void shouldDeleteCombinationSuccessfully() throws Exception {
            // Given
            when(colorCombinationService.getById(1L)).thenReturn(testCombination);
            doNothing().when(colorCombinationService).deleteCombination(1L);
            
            // When & Then
            mockMvc.perform(post("/combinations/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/combinations/"))
                .andExpect(flash().attributeExists("success"));
            
            verify(colorCombinationService).getById(1L);
            verify(colorCombinationService).deleteCombination(1L);
        }
        
        @Test
        @DisplayName("Debe manejar combinación no encontrada en eliminación")
        void shouldHandleNotFoundInDelete() throws Exception {
            // Given
            when(colorCombinationService.getById(999L))
                .thenThrow(new ColorCombinationNotFoundException(999L));
            
            // When & Then
            mockMvc.perform(post("/combinations/999/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/combinations/"))
                .andExpect(flash().attributeExists("error"));
        }
    }
    
    @Nested
    @DisplayName("Ver Combinación")
    class ViewCombinationTests {
        
        @Test
        @DisplayName("Debe mostrar detalles de combinación")
        void shouldDisplayCombinationDetails() throws Exception {
            // Given
            when(colorCombinationService.getById(1L)).thenReturn(testCombination);
            
            // When & Then
            mockMvc.perform(get("/combinations/1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("combination"));
            
            verify(colorCombinationService).getById(1L);
        }
        
        @Test
        @DisplayName("Debe manejar combinación no encontrada en visualización")
        void shouldHandleNotFoundInView() throws Exception {
            // Given
            when(colorCombinationService.getById(999L))
                .thenThrow(new ColorCombinationNotFoundException(999L));
            
            // When & Then
            mockMvc.perform(get("/combinations/999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/combinations/"))
                .andExpect(flash().attributeExists("error"));
        }
    }
    
    @Nested
    @DisplayName("Búsqueda AJAX")
    class AjaxSearchTests {
        
        @Test
        @DisplayName("Debe manejar búsqueda AJAX por término")
        void shouldHandleAjaxSearchByTerm() throws Exception {
            // Given
            when(colorCombinationService.searchCombinations("Ocean", null))
                .thenReturn(testCombinations);
            
            // When & Then
            mockMvc.perform(get("/combinations/search")
                    .param("term", "Ocean"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("combinations"));
            
            verify(colorCombinationService).searchCombinations("Ocean", null);
        }
        
        @Test
        @DisplayName("Debe manejar búsqueda AJAX por valor hexadecimal")
        void shouldHandleAjaxSearchByHexValue() throws Exception {
            // Given
            when(colorCombinationService.findByHexValue("FF6B35"))
                .thenReturn(testCombinations);
            
            // When & Then
            mockMvc.perform(get("/combinations/search")
                    .param("hexValue", "FF6B35"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("combinations"));
            
            verify(colorCombinationService).findByHexValue("FF6B35");
        }
    }
    
    @Nested
    @DisplayName("Manejo de Errores")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Debe manejar errores inesperados en creación")
        void shouldHandleUnexpectedErrorsInCreation() throws Exception {
            // Given
            when(colorCombinationService.createCombination(any(ColorCombinationForm.class)))
                .thenThrow(new RuntimeException("Unexpected error"));
            when(colorCombinationService.findAllCombinations()).thenReturn(testCombinations);
            
            // When & Then
            mockMvc.perform(post("/combinations/create")
                    .param("name", "Test Combination")
                    .param("colorCount", "2")
                    .param("colors[0].hexValue", "0077BE")
                    .param("colors[0].position", "1")
                    .param("colors[1].hexValue", "87CEEB")
                    .param("colors[1].position", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
        }
        
        @Test
        @DisplayName("Debe manejar errores en eliminación")
        void shouldHandleErrorsInDelete() throws Exception {
            // Given
            when(colorCombinationService.getById(1L)).thenReturn(testCombination);
            doThrow(new RuntimeException("Delete error"))
                .when(colorCombinationService).deleteCombination(1L);
            
            // When & Then
            mockMvc.perform(post("/combinations/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/combinations/"))
                .andExpect(flash().attributeExists("error"));
        }
    }
}