package dev.kreaker.kolors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.kreaker.kolors.exception.ColorAdditionException;
import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.exception.ColorRemovalException;
import dev.kreaker.kolors.exception.EmptyCombinationException;
import dev.kreaker.kolors.exception.InvalidColorCountException;
import dev.kreaker.kolors.exception.InvalidColorFormatException;
import dev.kreaker.kolors.service.ColorCombinationService;
import jakarta.validation.ValidationException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Tests unitarios para GlobalExceptionHandler */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @Mock private ColorCombinationService colorCombinationService;

    @Mock private MessageSource messageSource;

    @Mock private Model model;

    @Mock private RedirectAttributes redirectAttributes;

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler(colorCombinationService, messageSource);

        // Setup default message source behavior with lenient stubbing
        Mockito.lenient()
                .when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // Return the key as default
    }

    @Test
    @DisplayName("Should handle ColorCombinationNotFoundException")
    void shouldHandleColorCombinationNotFoundException() {
        // Given
        ColorCombinationNotFoundException exception = new ColorCombinationNotFoundException(1L);
        when(messageSource.getMessage(
                        eq("error.combination.notfound"), any(), anyString(), any(Locale.class)))
                .thenReturn("Combinación de colores no encontrada");

        // When
        String result =
                exceptionHandler.handleColorCombinationNotFound(
                        exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(eq("error"), eq("Combinación de colores no encontrada"));
    }

    @Test
    @DisplayName("Should handle ColorCombinationValidationException")
    void shouldHandleColorCombinationValidationException() {
        // Given
        List<String> validationErrors = Arrays.asList("Error 1", "Error 2");
        ColorCombinationValidationException exception =
                new ColorCombinationValidationException(validationErrors);

        // Mock service calls
        when(colorCombinationService.findAllCombinations()).thenReturn(List.of());
        when(colorCombinationService.getStatistics())
                .thenReturn(new ColorCombinationService.CombinationStatistics(0, 0, 0, 0));
        when(messageSource.getMessage(
                        eq("error.validation.multiple"), any(), anyString(), any(Locale.class)))
                .thenReturn("Errores de validación: Error 1, Error 2");

        // When
        String result = exceptionHandler.handleColorCombinationValidation(exception, model);

        // Then
        assertEquals("combinations/index", result);
        verify(model).addAttribute(eq("error"), eq("Errores de validación: Error 1, Error 2"));
        verify(model).addAttribute(eq("validationErrors"), eq(validationErrors));
    }

    @Test
    @DisplayName("Should handle InvalidColorFormatException")
    void shouldHandleInvalidColorFormatException() {
        // Given
        InvalidColorFormatException exception = InvalidColorFormatException.forHexValue("INVALID");
        when(messageSource.getMessage(
                        eq("error.color.invalid.hex"), any(), anyString(), any(Locale.class)))
                .thenReturn("Formato de color hexadecimal inválido");

        // When
        String result =
                exceptionHandler.handleInvalidColorFormat(exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(eq("error"), eq("Formato de color hexadecimal inválido"));
    }

    @Test
    @DisplayName("Should handle InvalidColorCountException")
    void shouldHandleInvalidColorCountException() {
        // Given
        InvalidColorCountException exception = new InvalidColorCountException("Invalid count");
        when(messageSource.getMessage(
                        eq("error.combination.colors.required"),
                        any(),
                        anyString(),
                        any(Locale.class)))
                .thenReturn("Debe agregar al menos un color a la combinación");

        // When
        String result =
                exceptionHandler.handleInvalidColorCount(exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(
                        eq("error"), eq("Debe agregar al menos un color a la combinación"));
    }

    @Test
    @DisplayName("Should handle ColorAdditionException")
    void shouldHandleColorAdditionException() {
        // Given
        ColorAdditionException exception = ColorAdditionException.forCombination(1L, "FF0000");
        when(messageSource.getMessage(
                        eq("error.combination.color.add.failed"),
                        any(),
                        anyString(),
                        any(Locale.class)))
                .thenReturn("No se pudo agregar el color a la combinación");

        // When
        String result = exceptionHandler.handleColorAddition(exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(eq("error"), eq("No se pudo agregar el color a la combinación"));
    }

    @Test
    @DisplayName("Should handle ColorRemovalException")
    void shouldHandleColorRemovalException() {
        // Given
        ColorRemovalException exception = ColorRemovalException.forCombination(1L, 1);
        when(messageSource.getMessage(
                        eq("error.combination.color.remove.failed"),
                        any(),
                        anyString(),
                        any(Locale.class)))
                .thenReturn("No se pudo remover el color de la combinación");

        // When
        String result = exceptionHandler.handleColorRemoval(exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(
                        eq("error"), eq("No se pudo remover el color de la combinación"));
    }

    @Test
    @DisplayName("Should handle EmptyCombinationException")
    void shouldHandleEmptyCombinationException() {
        // Given
        EmptyCombinationException exception = EmptyCombinationException.forCreation();
        when(messageSource.getMessage(
                        eq("error.combination.colors.empty"),
                        any(),
                        anyString(),
                        any(Locale.class)))
                .thenReturn("La combinación no puede estar vacía");

        // When
        String result =
                exceptionHandler.handleEmptyCombination(exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(eq("error"), eq("La combinación no puede estar vacía"));
    }

    @Test
    @DisplayName("Should handle ValidationException")
    void shouldHandleValidationException() {
        // Given
        ValidationException exception = new ValidationException("Bean validation error");

        // Mock service calls
        when(colorCombinationService.findAllCombinations()).thenReturn(List.of());
        when(colorCombinationService.getStatistics())
                .thenReturn(new ColorCombinationService.CombinationStatistics(0, 0, 0, 0));
        when(messageSource.getMessage(
                        eq("error.validation.bean"), any(), anyString(), any(Locale.class)))
                .thenReturn("Error de validación: Bean validation error");

        // When
        String result = exceptionHandler.handleValidation(exception, model);

        // Then
        assertEquals("combinations/index", result);
        verify(model).addAttribute(eq("error"), eq("Error de validación: Bean validation error"));
    }

    @Test
    @DisplayName("Should handle database errors")
    void shouldHandleDatabaseErrors() {
        // Given
        org.springframework.dao.DataAccessException exception =
                new org.springframework.dao.DataAccessResourceFailureException("DB error");
        when(messageSource.getMessage(
                        eq("error.database.generic"), any(), anyString(), any(Locale.class)))
                .thenReturn("Error de base de datos. Por favor, inténtelo de nuevo más tarde");

        // When
        String result = exceptionHandler.handleDatabaseError(exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(
                        eq("error"),
                        eq("Error de base de datos. Por favor, inténtelo de nuevo más tarde"));
    }

    @Test
    @DisplayName("Should handle illegal arguments")
    void shouldHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
        when(messageSource.getMessage(
                        eq("error.server.illegal.argument"), any(), anyString(), any(Locale.class)))
                .thenReturn("Datos inválidos proporcionados: Invalid argument");

        // When
        String result =
                exceptionHandler.handleIllegalArgument(exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(
                        eq("error"), eq("Datos inválidos proporcionados: Invalid argument"));
    }

    @Test
    @DisplayName("Should rethrow specific exceptions in RuntimeException handler")
    void shouldRethrowSpecificExceptionsInRuntimeHandler() {
        // Given
        ColorCombinationNotFoundException specificException =
                new ColorCombinationNotFoundException(1L);

        // When & Then
        assertThrows(
                ColorCombinationNotFoundException.class,
                () -> {
                    exceptionHandler.handleRuntimeError(
                            specificException, model, redirectAttributes);
                });
    }

    @Test
    @DisplayName("Should handle generic RuntimeException")
    void shouldHandleGenericRuntimeException() {
        // Given
        RuntimeException exception = new RuntimeException("Generic runtime error");
        when(messageSource.getMessage(
                        eq("error.server.runtime"), any(), anyString(), any(Locale.class)))
                .thenReturn(
                        "Error del sistema. Por favor, contacte al administrador si el problema persiste");

        // When
        String result = exceptionHandler.handleRuntimeError(exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(
                        eq("error"),
                        eq(
                                "Error del sistema. Por favor, contacte al administrador si el problema persiste"));
    }

    @Test
    @DisplayName("Should handle generic exceptions")
    void shouldHandleGenericExceptions() {
        // Given
        Exception exception = new Exception("Generic error");
        when(messageSource.getMessage(
                        eq("error.server.generic"), any(), anyString(), any(Locale.class)))
                .thenReturn("Ha ocurrido un error inesperado. Por favor, inténtelo de nuevo");

        // When
        String result = exceptionHandler.handleGenericError(exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(
                        eq("error"),
                        eq("Ha ocurrido un error inesperado. Por favor, inténtelo de nuevo"));
    }

    @Test
    @DisplayName("Should handle service error when preparing error model")
    void shouldHandleServiceErrorWhenPreparingErrorModel() {
        // Given
        ColorCombinationValidationException exception =
                new ColorCombinationValidationException("Test error");
        when(colorCombinationService.findAllCombinations())
                .thenThrow(new RuntimeException("Service error"));

        // When
        String result = exceptionHandler.handleColorCombinationValidation(exception, model);

        // Then
        assertEquals("combinations/index", result);
        verify(model).addAttribute(eq("combinations"), eq(List.of()));
        verify(model).addAttribute(eq("combinationForm"), any(ColorCombinationForm.class));
    }

    @Test
    @DisplayName("Should handle ColorAdditionException with duplicate color")
    void shouldHandleColorAdditionExceptionWithDuplicateColor() {
        // Given
        ColorAdditionException exception = ColorAdditionException.forDuplicateColor("FF0000");
        when(messageSource.getMessage(
                        eq("error.combination.color.add.failed"),
                        any(),
                        anyString(),
                        any(Locale.class)))
                .thenReturn("No se pudo agregar el color a la combinación");

        // When
        String result = exceptionHandler.handleColorAddition(exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(eq("error"), eq("No se pudo agregar el color a la combinación"));
    }

    @Test
    @DisplayName("Should handle ColorRemovalException for last color")
    void shouldHandleColorRemovalExceptionForLastColor() {
        // Given
        ColorRemovalException exception = ColorRemovalException.forLastColor();
        when(messageSource.getMessage(
                        eq("error.combination.color.remove.failed"),
                        any(),
                        anyString(),
                        any(Locale.class)))
                .thenReturn("No se pudo remover el color de la combinación");

        // When
        String result = exceptionHandler.handleColorRemoval(exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(
                        eq("error"), eq("No se pudo remover el color de la combinación"));
    }

    @Test
    @DisplayName("Should handle EmptyCombinationException for update")
    void shouldHandleEmptyCombinationExceptionForUpdate() {
        // Given
        EmptyCombinationException exception = EmptyCombinationException.forUpdate(1L);
        when(messageSource.getMessage(
                        eq("error.combination.colors.empty"),
                        any(),
                        anyString(),
                        any(Locale.class)))
                .thenReturn("La combinación no puede estar vacía");

        // When
        String result =
                exceptionHandler.handleEmptyCombination(exception, model, redirectAttributes);

        // Then
        assertEquals("redirect:/combinations/", result);
        verify(redirectAttributes)
                .addFlashAttribute(eq("error"), eq("La combinación no puede estar vacía"));
    }
}
