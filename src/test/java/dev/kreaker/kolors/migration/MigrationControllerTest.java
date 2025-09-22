package dev.kreaker.kolors.migration;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(MigrationController.class)
class MigrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatabaseMigrationService migrationService;

    @Autowired
    private ObjectMapper objectMapper;

    private MigrationStatistics mockStatistics;
    private MigrationResult mockResult;

    @BeforeEach
    void setUp() {
        mockStatistics = new MigrationStatistics();
        mockStatistics.setLegacyRecordCount(10);
        mockStatistics.setSingleColorCombinationCount(8);
        mockStatistics.setTotalCombinationCount(15);
        mockStatistics.setMigrationStatus(MigrationStatus.COMPLETED);
        mockStatistics.setLastMigrationTime(LocalDateTime.now());
        mockStatistics.setLastMigrationSuccess(true);

        mockResult = new MigrationResult(true, 10, 8);
        mockResult.setSummary("Migration completed successfully");
    }

    @Test
    void shouldShowMigrationStatusPage() throws Exception {
        // Given
        when(migrationService.getMigrationStatistics()).thenReturn(mockStatistics);
        when(migrationService.getMigrationStatus()).thenReturn(MigrationStatus.COMPLETED);
        when(migrationService.isLegacyDataPresent()).thenReturn(true);
        when(migrationService.getLastMigrationResult()).thenReturn(Optional.of(mockResult));

        // When & Then
        mockMvc.perform(get("/admin/migration/status"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/migration-status"))
                .andExpect(model().attributeExists("statistics"))
                .andExpect(model().attributeExists("migrationStatus"))
                .andExpect(model().attributeExists("hasLegacyData"))
                .andExpect(model().attributeExists("lastMigrationResult"))
                .andExpect(model().attribute("hasLegacyData", true))
                .andExpect(model().attribute("migrationStatus", MigrationStatus.COMPLETED));
    }

    @Test
    void shouldHandleErrorInStatusPage() throws Exception {
        // Given
        when(migrationService.getMigrationStatistics()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/admin/migration/status"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/migration-status"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", containsString("Error loading migration status")));
    }

    @Test
    void shouldPerformMigrationSuccessfully() throws Exception {
        // Given
        when(migrationService.getMigrationStatus()).thenReturn(MigrationStatus.NOT_STARTED);
        when(migrationService.isLegacyDataPresent()).thenReturn(true);
        when(migrationService.createBackup()).thenReturn(true);
        when(migrationService.migrateLegacyData()).thenReturn(mockResult);

        // When & Then
        mockMvc.perform(post("/admin/migration/migrate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/migration/status"))
                .andExpect(flash().attributeExists("success"))
                .andExpect(flash().attribute("success", containsString("Migration completed successfully")));

        verify(migrationService).createBackup();
        verify(migrationService).migrateLegacyData();
    }

    @Test
    void shouldPreventMigrationWhenInProgress() throws Exception {
        // Given
        when(migrationService.getMigrationStatus()).thenReturn(MigrationStatus.IN_PROGRESS);

        // When & Then
        mockMvc.perform(post("/admin/migration/migrate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/migration/status"))
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attribute("error", "Migration is already in progress"));

        verify(migrationService, never()).migrateLegacyData();
    }

    @Test
    void shouldHandleNoLegacyDataDuringMigration() throws Exception {
        // Given
        when(migrationService.getMigrationStatus()).thenReturn(MigrationStatus.NOT_STARTED);
        when(migrationService.isLegacyDataPresent()).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/admin/migration/migrate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/migration/status"))
                .andExpect(flash().attributeExists("warning"))
                .andExpect(flash().attribute("warning", "No legacy data found to migrate"));

        verify(migrationService, never()).migrateLegacyData();
    }

    @Test
    void shouldHandleMigrationWithErrors() throws Exception {
        // Given
        MigrationResult resultWithErrors = new MigrationResult(true, 10, 8);
        resultWithErrors.setFailedRecords(2);
        resultWithErrors.setSummary("Migration completed with 2 errors");

        when(migrationService.getMigrationStatus()).thenReturn(MigrationStatus.NOT_STARTED);
        when(migrationService.isLegacyDataPresent()).thenReturn(true);
        when(migrationService.createBackup()).thenReturn(true);
        when(migrationService.migrateLegacyData()).thenReturn(resultWithErrors);

        // When & Then
        mockMvc.perform(post("/admin/migration/migrate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/migration/status"))
                .andExpect(flash().attributeExists("warning"))
                .andExpect(flash().attribute("warning", containsString("Migration completed with some errors")));
    }

    @Test
    void shouldHandleMigrationFailure() throws Exception {
        // Given
        MigrationResult failedResult = new MigrationResult(false, 10, 0);
        failedResult.setSummary("Migration failed completely");

        when(migrationService.getMigrationStatus()).thenReturn(MigrationStatus.NOT_STARTED);
        when(migrationService.isLegacyDataPresent()).thenReturn(true);
        when(migrationService.createBackup()).thenReturn(true);
        when(migrationService.migrateLegacyData()).thenReturn(failedResult);

        // When & Then
        mockMvc.perform(post("/admin/migration/migrate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/migration/status"))
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attribute("error", containsString("Migration failed")));
    }

    @Test
    void shouldHandleBackupFailure() throws Exception {
        // Given
        when(migrationService.getMigrationStatus()).thenReturn(MigrationStatus.NOT_STARTED);
        when(migrationService.isLegacyDataPresent()).thenReturn(true);
        when(migrationService.createBackup()).thenReturn(false);
        when(migrationService.migrateLegacyData()).thenReturn(mockResult);

        // When & Then
        mockMvc.perform(post("/admin/migration/migrate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/migration/status"))
                .andExpect(flash().attributeExists("success"));

        verify(migrationService).migrateLegacyData(); // Should still proceed
    }

    @Test
    void shouldReturnMigrationStatisticsAsJson() throws Exception {
        // Given
        when(migrationService.getMigrationStatistics()).thenReturn(mockStatistics);

        // When & Then
        mockMvc.perform(get("/admin/migration/api/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.legacyRecordCount", is(10)))
                .andExpect(jsonPath("$.singleColorCombinationCount", is(8)))
                .andExpect(jsonPath("$.totalCombinationCount", is(15)))
                .andExpect(jsonPath("$.migrationStatus", is("COMPLETED")))
                .andExpect(jsonPath("$.lastMigrationSuccess", is(true)));
    }

    @Test
    void shouldHandleErrorInStatisticsApi() throws Exception {
        // Given
        when(migrationService.getMigrationStatistics()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/admin/migration/api/statistics"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldCheckLegacyDataViaApi() throws Exception {
        // Given
        when(migrationService.isLegacyDataPresent()).thenReturn(true);
        when(migrationService.getMigrationStatus()).thenReturn(MigrationStatus.NOT_STARTED);

        // When & Then
        mockMvc.perform(get("/admin/migration/api/legacy-data-check"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hasLegacyData", is(true)))
                .andExpect(jsonPath("$.migrationStatus", is("NOT_STARTED")))
                .andExpect(jsonPath("$.migrationNeeded", is(true)));
    }

    @Test
    void shouldPerformMigrationViaApi() throws Exception {
        // Given
        when(migrationService.getMigrationStatus()).thenReturn(MigrationStatus.NOT_STARTED);
        when(migrationService.isLegacyDataPresent()).thenReturn(true);
        when(migrationService.migrateLegacyData()).thenReturn(mockResult);

        // When & Then
        mockMvc.perform(post("/admin/migration/api/migrate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.totalLegacyRecords", is(10)))
                .andExpect(jsonPath("$.migratedRecords", is(8)));

        verify(migrationService).migrateLegacyData();
    }

    @Test
    void shouldPreventApiMigrationWhenInProgress() throws Exception {
        // Given
        when(migrationService.getMigrationStatus()).thenReturn(MigrationStatus.IN_PROGRESS);

        // When & Then
        mockMvc.perform(post("/admin/migration/api/migrate"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors[0]", containsString("Migration is already in progress")));

        verify(migrationService, never()).migrateLegacyData();
    }

    @Test
    void shouldHandleNoLegacyDataInApiMigration() throws Exception {
        // Given
        when(migrationService.getMigrationStatus()).thenReturn(MigrationStatus.NOT_STARTED);
        when(migrationService.isLegacyDataPresent()).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/admin/migration/api/migrate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.warnings[0]", containsString("No legacy data found to migrate")));

        verify(migrationService, never()).migrateLegacyData();
    }

    @Test
    void shouldResetMigrationStatusViaApi() throws Exception {
        // Given
        doNothing().when(migrationService).resetMigrationStatus();

        // When & Then
        mockMvc.perform(post("/admin/migration/api/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("Migration status reset successfully"));

        verify(migrationService).resetMigrationStatus();
    }

    @Test
    void shouldHandleErrorInResetApi() throws Exception {
        // Given
        doThrow(new RuntimeException("Reset error")).when(migrationService).resetMigrationStatus();

        // When & Then
        mockMvc.perform(post("/admin/migration/api/reset"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error resetting migration status")));
    }
}
