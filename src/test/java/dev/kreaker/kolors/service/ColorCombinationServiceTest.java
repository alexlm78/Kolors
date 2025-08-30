package dev.kreaker.kolors.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dev.kreaker.kolors.ColorCombination;
import dev.kreaker.kolors.ColorCombinationForm;
import dev.kreaker.kolors.ColorCombinationRepository;
import dev.kreaker.kolors.ColorForm;
import dev.kreaker.kolors.ColorInCombination;
import dev.kreaker.kolors.ColorInCombinationRepository;
import dev.kreaker.kolors.exception.ColorCombinationNotFoundException;
import dev.kreaker.kolors.exception.ColorCombinationValidationException;
import dev.kreaker.kolors.exception.InvalidColorFormatException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ColorCombinationService Tests")
class ColorCombinationServiceTest {
    
    @Mock
    private ColorCombinationRepository colorCombinationRepository;
    
    @Mock
    private ColorInCombinationRepository colorInCombinationRepository;
    
    @InjectMocks
    private ColorCombinationService colorCombinationService;
    
    private ColorCombinationForm validForm;
    private ColorCombination validCombination;
    
    @BeforeEach
    void setUp() {
        // Create valid form
        validForm = new ColorCombinationForm();
        validForm.setName("Test Combination");
        validForm.setColorCount(3);
        validForm.setColors(Arrays.asList(
            new ColorForm("FF0000", 1),
            new ColorForm("00FF00", 2),
            new ColorForm("0000FF", 3)
        ));
        
        // Create valid combination
        validCombination = new ColorCombination("Test Combination", 3);
        validCombination.setId(1L);
        validCombination.setCreatedAt(LocalDateTime.now());
        validCombination.addColor(new ColorInCombination("FF0000", 1));
        validCombination.addColor(new ColorInCombination("00FF00", 2));
        validCombination.addColor(new ColorInCombination("0000FF", 3));
    }
    
    @Nested
    @DisplayName("Create Combination Tests")
    class CreateCombinationTests {
        
        @Test
        @DisplayName("Should create combination successfully with valid form")
        void shouldCreateCombinationSuccessfully() {
            // Given
            when(colorCombinationRepository.save(any(ColorCombination.class)))
                .thenReturn(validCombination);
            
            // When
            ColorCombination result = colorCombinationService.createCombination(validForm);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Combinación de Prueba");
            assertThat(result.getColorCount()).isEqualTo(3);
            assertThat(result.getColors()).hasSize(3);
            
            verify(colorCombinationRepository).save(any(ColorCombination.class));
        }
        
        @Test
        @DisplayName("Should throw validation exception with invalid name")
        void shouldThrowValidationExceptionWithInvalidName() {
            // Given
            validForm.setName("AB"); // Muy corto
            
            // When & Then
            assertThatThrownBy(() -> colorCombinationService.createCombination(validForm))
                .isInstanceOf(ColorCombinationValidationException.class)
                .hasMessageContaining("Name must have at least 3 characters");
            
            verify(colorCombinationRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("Should throw validation exception with invalid color count")
        void shouldThrowValidationExceptionWithInvalidColorCount() {
            // Given
            validForm.setColorCount(5); // Outside valid range
            
            // When & Then
            assertThatThrownBy(() -> colorCombinationService.createCombination(validForm))
                .isInstanceOf(ColorCombinationValidationException.class)
                .hasMessageContaining("Must specify between 2 and 4 colors");
            
            verify(colorCombinationRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("Should throw validation exception with invalid hex color")
        void shouldThrowValidationExceptionWithInvalidHexColor() {
            // Given
            validForm.getColors().get(0).setHexValue("INVALID"); // Invalid format
            
            // When & Then
            assertThatThrownBy(() -> colorCombinationService.createCombination(validForm))
                .isInstanceOf(ColorCombinationValidationException.class)
                .hasMessageContaining("invalid hexadecimal format");
            
            verify(colorCombinationRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("Should throw validation exception with mismatched color count")
        void shouldThrowValidationExceptionWithMismatchedColorCount() {
            // Given - Create a form with valid hex colors but mismatched count
            ColorCombinationForm form = new ColorCombinationForm();
            form.setName("Test Combination");
            form.setColorCount(2); // Expecting 2 colors
            form.setColors(Arrays.asList(
                new ColorForm("FF0000", 1),
                new ColorForm("00FF00", 2),
                new ColorForm("0000FF", 3) // But providing 3 colors
            ));
            
            // When & Then
            assertThatThrownBy(() -> colorCombinationService.createCombination(form))
                .isInstanceOf(ColorCombinationValidationException.class)
                .hasMessageContaining("no coincide con el especificado");
            
            verify(colorCombinationRepository, never()).save(any());
        }
    }
    
    @Nested
    @DisplayName("Read Combination Tests")
    class ReadCombinationTests {
        
        @Test
        @DisplayName("Should find all combinations")
        void shouldFindAllCombinations() {
            // Given
            List<ColorCombination> combinations = Arrays.asList(validCombination);
            when(colorCombinationRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(combinations);
            
            // When
            List<ColorCombination> result = colorCombinationService.findAllCombinations();
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(validCombination);
            
            verify(colorCombinationRepository).findAllByOrderByCreatedAtDesc();
        }
        
        @Test
        @DisplayName("Should find combinations with pagination")
        void shouldFindCombinationsWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ColorCombination> page = new PageImpl<>(Arrays.asList(validCombination));
            when(colorCombinationRepository.findAllByOrderByNameAsc(pageable))
                .thenReturn(page);
            
            // When
            Page<ColorCombination> result = colorCombinationService.findAllCombinations(pageable);
            
            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(validCombination);
            
            verify(colorCombinationRepository).findAllByOrderByNameAsc(pageable);
        }
        
        @Test
        @DisplayName("Should find combination by ID")
        void shouldFindCombinationById() {
            // Given
            Long id = 1L;
            when(colorCombinationRepository.findById(id))
                .thenReturn(Optional.of(validCombination));
            
            // When
            Optional<ColorCombination> result = colorCombinationService.findById(id);
            
            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(validCombination);
            
            verify(colorCombinationRepository).findById(id);
        }
        
        @Test
        @DisplayName("Should get combination by ID or throw exception")
        void shouldGetCombinationByIdOrThrowException() {
            // Given
            Long id = 1L;
            when(colorCombinationRepository.findById(id))
                .thenReturn(Optional.of(validCombination));
            
            // When
            ColorCombination result = colorCombinationService.getById(id);
            
            // Then
            assertThat(result).isEqualTo(validCombination);
            
            verify(colorCombinationRepository).findById(id);
        }
        
        @Test
        @DisplayName("Should throw exception when combination not found by ID")
        void shouldThrowExceptionWhenCombinationNotFoundById() {
            // Given
            Long id = 999L;
            when(colorCombinationRepository.findById(id))
                .thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> colorCombinationService.getById(id))
                .isInstanceOf(ColorCombinationNotFoundException.class)
                .hasMessageContaining("999");
            
            verify(colorCombinationRepository).findById(id);
        }
        
        @Test
        @DisplayName("Should search combinations by name")
        void shouldSearchCombinationsByName() {
            // Given
            String searchTerm = "Prueba";
            when(colorCombinationRepository.findByNameContainingIgnoreCase(searchTerm))
                .thenReturn(Arrays.asList(validCombination));
            
            // When
            List<ColorCombination> result = colorCombinationService.searchCombinations(searchTerm, null);
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(validCombination);
            
            verify(colorCombinationRepository).findByNameContainingIgnoreCase(searchTerm);
        }
        
        @Test
        @DisplayName("Should search combinations by color count")
        void shouldSearchCombinationsByColorCount() {
            // Given
            Integer colorCount = 3;
            when(colorCombinationRepository.findByColorCount(colorCount))
                .thenReturn(Arrays.asList(validCombination));
            
            // When
            List<ColorCombination> result = colorCombinationService.searchCombinations(null, colorCount);
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(validCombination);
            
            verify(colorCombinationRepository).findByColorCount(colorCount);
        }
        
        @Test
        @DisplayName("Should find combinations by hex value")
        void shouldFindCombinationsByHexValue() {
            // Given
            String hexValue = "FF0000";
            when(colorCombinationRepository.findByContainingHexValue("FF0000"))
                .thenReturn(Arrays.asList(validCombination));
            
            // When
            List<ColorCombination> result = colorCombinationService.findByHexValue(hexValue);
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(validCombination);
            
            verify(colorCombinationRepository).findByContainingHexValue("FF0000");
        }
        
        @Test
        @DisplayName("Should throw exception with invalid hex value")
        void shouldThrowExceptionWithInvalidHexValue() {
            // Given
            String invalidHex = "INVALID";
            
            // When & Then
            assertThatThrownBy(() -> colorCombinationService.findByHexValue(invalidHex))
                .isInstanceOf(InvalidColorFormatException.class)
                .hasMessageContaining("INVALID");
            
            verify(colorCombinationRepository, never()).findByContainingHexValue(any());
        }
    }
    
    @Nested
    @DisplayName("Update Combination Tests")
    class UpdateCombinationTests {
        
        @Test
        @DisplayName("Should update combination successfully")
        void shouldUpdateCombinationSuccessfully() {
            // Given
            Long id = 1L;
            ColorCombinationForm updateForm = new ColorCombinationForm();
            updateForm.setName("Combinación Actualizada");
            updateForm.setColorCount(2);
            updateForm.setColors(Arrays.asList(
                new ColorForm("FFFFFF", 1),
                new ColorForm("000000", 2)
            ));
            
            when(colorCombinationRepository.findById(id))
                .thenReturn(Optional.of(validCombination));
            when(colorCombinationRepository.save(any(ColorCombination.class)))
                .thenReturn(validCombination);
            
            // When
            ColorCombination result = colorCombinationService.updateCombination(id, updateForm);
            
            // Then
            assertThat(result).isNotNull();
            
            verify(colorCombinationRepository).findById(id);
            verify(colorCombinationRepository).save(any(ColorCombination.class));
        }
        
        @Test
        @DisplayName("Should throw exception when updating non-existent combination")
        void shouldThrowExceptionWhenUpdatingNonExistentCombination() {
            // Given
            Long id = 999L;
            when(colorCombinationRepository.findById(id))
                .thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> colorCombinationService.updateCombination(id, validForm))
                .isInstanceOf(ColorCombinationNotFoundException.class)
                .hasMessageContaining("999");
            
            verify(colorCombinationRepository).findById(id);
            verify(colorCombinationRepository, never()).save(any());
        }
    }
    
    @Nested
    @DisplayName("Delete Combination Tests")
    class DeleteCombinationTests {
        
        @Test
        @DisplayName("Should delete combination successfully")
        void shouldDeleteCombinationSuccessfully() {
            // Given
            Long id = 1L;
            when(colorCombinationRepository.existsById(id)).thenReturn(true);
            
            // When
            colorCombinationService.deleteCombination(id);
            
            // Then
            verify(colorCombinationRepository).existsById(id);
            verify(colorCombinationRepository).deleteById(id);
        }
        
        @Test
        @DisplayName("Should throw exception when deleting non-existent combination")
        void shouldThrowExceptionWhenDeletingNonExistentCombination() {
            // Given
            Long id = 999L;
            when(colorCombinationRepository.existsById(id)).thenReturn(false);
            
            // When & Then
            assertThatThrownBy(() -> colorCombinationService.deleteCombination(id))
                .isInstanceOf(ColorCombinationNotFoundException.class)
                .hasMessageContaining("999");
            
            verify(colorCombinationRepository).existsById(id);
            verify(colorCombinationRepository, never()).deleteById(any());
        }
    }
    
    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {
        
        @Test
        @DisplayName("Should validate hex color correctly")
        void shouldValidateHexColorCorrectly() {
            // Valid hex colors
            assertThat(colorCombinationService.isValidHexColor("FF0000")).isTrue();
            assertThat(colorCombinationService.isValidHexColor("00ff00")).isTrue();
            assertThat(colorCombinationService.isValidHexColor("123ABC")).isTrue();
            
            // Invalid hex colors
            assertThat(colorCombinationService.isValidHexColor("INVALID")).isFalse();
            assertThat(colorCombinationService.isValidHexColor("FF00")).isFalse();
            assertThat(colorCombinationService.isValidHexColor("FF0000G")).isFalse();
            assertThat(colorCombinationService.isValidHexColor(null)).isFalse();
            assertThat(colorCombinationService.isValidHexColor("")).isFalse();
        }
        
        @Test
        @DisplayName("Should validate hex colors list correctly")
        void shouldValidateHexColorsListCorrectly() {
            // Valid list
            List<String> validColors = Arrays.asList("FF0000", "00FF00", "0000FF");
            assertThat(colorCombinationService.validateHexColors(validColors)).isTrue();
            
            // Invalid list
            List<String> invalidColors = Arrays.asList("FF0000", "INVALID", "0000FF");
            assertThat(colorCombinationService.validateHexColors(invalidColors)).isFalse();
            
            // Null or empty list
            assertThat(colorCombinationService.validateHexColors(null)).isFalse();
            assertThat(colorCombinationService.validateHexColors(Arrays.asList())).isFalse();
        }
        
        @Test
        @DisplayName("Should validate color count correctly")
        void shouldValidateColorCountCorrectly() {
            List<ColorForm> colors = Arrays.asList(
                new ColorForm("FF0000", 1),
                new ColorForm("00FF00", 2)
            );
            
            // Valid count
            assertThat(colorCombinationService.validateColorCount(2, colors)).isTrue();
            
            // Invalid count
            assertThat(colorCombinationService.validateColorCount(3, colors)).isFalse();
            assertThat(colorCombinationService.validateColorCount(1, colors)).isFalse();
            assertThat(colorCombinationService.validateColorCount(5, colors)).isFalse();
            
            // Null parameters
            assertThat(colorCombinationService.validateColorCount(null, colors)).isFalse();
            assertThat(colorCombinationService.validateColorCount(2, null)).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Utility Tests")
    class UtilityTests {
        
        @Test
        @DisplayName("Should convert entity to form correctly")
        void shouldConvertEntityToFormCorrectly() {
            // When
            ColorCombinationForm result = colorCombinationService.convertToForm(validCombination);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(validCombination.getName());
            assertThat(result.getColorCount()).isEqualTo(validCombination.getColorCount());
            assertThat(result.getColors()).hasSize(validCombination.getColors().size());
        }
        
        @Test
        @DisplayName("Should return null when converting null entity")
        void shouldReturnNullWhenConvertingNullEntity() {
            // When
            ColorCombinationForm result = colorCombinationService.convertToForm(null);
            
            // Then
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("Should check if combination exists by name")
        void shouldCheckIfCombinationExistsByName() {
            // Given
            String name = "Test Combination";
            when(colorCombinationRepository.existsByNameIgnoreCase(name)).thenReturn(true);
            
            // When
            boolean result = colorCombinationService.existsByName(name);
            
            // Then
            assertThat(result).isTrue();
            verify(colorCombinationRepository).existsByNameIgnoreCase(name);
        }
        
        @Test
        @DisplayName("Should get statistics correctly")
        void shouldGetStatisticsCorrectly() {
            // Given
            when(colorCombinationRepository.count()).thenReturn(10L);
            when(colorCombinationRepository.countByColorCount(2)).thenReturn(3L);
            when(colorCombinationRepository.countByColorCount(3)).thenReturn(4L);
            when(colorCombinationRepository.countByColorCount(4)).thenReturn(3L);
            
            // When
            ColorCombinationService.CombinationStatistics result = colorCombinationService.getStatistics();
            
            // Then
            assertThat(result.getTotalCombinations()).isEqualTo(10L);
            assertThat(result.getCombinationsWith2Colors()).isEqualTo(3L);
            assertThat(result.getCombinationsWith3Colors()).isEqualTo(4L);
            assertThat(result.getCombinationsWith4Colors()).isEqualTo(3L);
        }
        
        @Test
        @DisplayName("Should get recent combinations")
        void shouldGetRecentCombinations() {
            // Given
            int limit = 5;
            when(colorCombinationRepository.findMostRecent(any(Pageable.class)))
                .thenReturn(Arrays.asList(validCombination));
            
            // When
            List<ColorCombination> result = colorCombinationService.getRecentCombinations(limit);
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(validCombination);
            
            verify(colorCombinationRepository).findMostRecent(any(Pageable.class));
        }
    }
}