package dev.kreaker.kolors;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.exception.InvalidColorFormatException;
import dev.kreaker.kolors.service.ColorCombinationService;

/**
 * Tests unitarios para GlobalExceptionHandler
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {
    
    @Mock
    private ColorCombinationService colorCombinationService;
    
    @Mock
    private Model model;
    
    @Mock
    private RedirectAttributes redirectAttributes;
    
    private GlobalExceptionHandler exceptionHandler;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler(colorCombinationService);
    }
    
    @Test
    @DisplayName("Should handle ColorCombinationNotFoundException")
    void shouldHandleColorCombinationNotFoundException() {
        // Given
        ColorCombinationNotFoundException exception = new ColorCombinationNotFoundException(1L);
        
        // When
        String result = exceptionHandler.handleColorCombinationNotFound(exception, model, redirectAttributes);
        
        // Then
        assertEquals("redirect:/combinations/", result);
    }
    
    @Test
    @DisplayName("Should handle ColorCombinationValidationException")
    void shouldHandleColorCombinationValidationException() {
        // Given
        List<String> validationErrors = Arrays.asList("Error 1", "Error 2");
        ColorCombinationValidationException exception = new ColorCombinationValidationException(validationErrors);
        
        // Mock service calls
        when(colorCombinationService.findAllCombinations()).thenReturn(List.of());
        when(colorCombinationService.getStatistics())
            .thenReturn(new ColorCombinationService.CombinationStatistics(0, 0, 0, 0));
        
        // When
        String result = exceptionHandler.handleColorCombinationValidation(exception, model);
        
        // Then
        assertEquals("combinations/index", result);
    }
    
    @Test
    @DisplayName("Should handle InvalidColorFormatException")
    void shouldHandleInvalidColorFormatException() {
        // Given
        InvalidColorFormatException exception = InvalidColorFormatException.forHexValue("INVALID");
        
        // When
        String result = exceptionHandler.handleInvalidColorFormat(exception, model, redirectAttributes);
        
        // Then
        assertEquals("redirect:/combinations/", result);
    }
    
    @Test
    @DisplayName("Should handle generic exceptions")
    void shouldHandleGenericExceptions() {
        // Given
        Exception exception = new Exception("Generic error");
        
        // When
        String result = exceptionHandler.handleGenericError(exception, model, redirectAttributes);
        
        // Then
        assertEquals("redirect:/combinations/", result);
    }
    
    @Test
    @DisplayName("Should handle database errors")
    void shouldHandleDatabaseErrors() {
        // Given
        org.springframework.dao.DataAccessException exception = 
            new org.springframework.dao.DataAccessResourceFailureException("DB error");
        
        // When
        String result = exceptionHandler.handleDatabaseError(exception, model, redirectAttributes);
        
        // Then
        assertEquals("redirect:/combinations/", result);
    }
    
    @Test
    @DisplayName("Should handle illegal arguments")
    void shouldHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
        
        // When
        String result = exceptionHandler.handleIllegalArgument(exception, model, redirectAttributes);
        
        // Then
        assertEquals("redirect:/combinations/", result);
    }
    
    @Test
    @DisplayName("Should rethrow specific exceptions in RuntimeException handler")
    void shouldRethrowSpecificExceptionsInRuntimeHandler() {
        // Given
        ColorCombinationNotFoundException specificException = new ColorCombinationNotFoundException(1L);
        
        // When & Then
        try {
            exceptionHandler.handleRuntimeError(specificException, model, redirectAttributes);
        } catch (ColorCombinationNotFoundException e) {
            assertEquals(specificException, e);
        }
    }
    
    @Test
    @DisplayName("Should handle generic RuntimeException")
    void shouldHandleGenericRuntimeException() {
        // Given
        RuntimeException exception = new RuntimeException("Generic runtime error");
        
        // When
        String result = exceptionHandler.handleRuntimeError(exception, model, redirectAttributes);
        
        // Then
        assertEquals("redirect:/combinations/", result);
    }
}