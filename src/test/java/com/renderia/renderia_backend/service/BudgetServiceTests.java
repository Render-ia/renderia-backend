package com.renderia.renderia_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.BudgetSettings;
import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.repository.BudgetSettingsRepository;
import com.renderia.renderia_backend.security.CurrentUser;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class BudgetServiceTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final CurrentUser engineer = new CurrentUser(7L, Role.ENGINEER);
    private AnalysisService analyses;
    private BudgetSettingsRepository settings;
    private BudgetService service;

    @BeforeEach
    void setUp() {
        analyses = mock(AnalysisService.class);
        settings = mock(BudgetSettingsRepository.class);
        service = new BudgetService(analyses, settings);
    }

    private static JsonNode json(String text) {
        return JSON.readTree(text);
    }

    @Test
    void savesPricesParametersAndAiuOfAnAnalysisTheUserCanEdit() {
        when(settings.findById(3L)).thenReturn(Optional.empty());

        JsonNode saved = service.save(engineer, 3L, json("{\"prices\":{\"masonry\":85000},\"aiu\":{\"profit\":0.05},\"params\":{\"wallHeight\":2.6}}"));

        verify(analyses).findManageable(engineer, 3L);
        ArgumentCaptor<BudgetSettings> row = ArgumentCaptor.forClass(BudgetSettings.class);
        verify(settings).save(row.capture());
        assertThat(row.getValue().getAiAnalysisId()).isEqualTo(3L);
        assertThat(row.getValue().getUpdatedBy()).isEqualTo(7L);
        assertThat(saved.get("prices").get("masonry").asDouble()).isEqualTo(85000);
        assertThat(saved.get("params").get("wallHeight").asDouble()).isEqualTo(2.6);
    }

    @Test
    void refusesNegativeHugeOrNonNumericValuesAndUnknownGroups() {
        for (String bad : new String[] {
                "{\"prices\":{\"masonry\":-1}}",
                "{\"prices\":{\"masonry\":1e13}}",
                "{\"prices\":{\"masonry\":\"mil\"}}",
                "{\"prices\":{\"<script>\":1}}",
                "{\"hack\":{}}",
                "[1,2]",
                "{\"prices\":[1]}"}) {
            assertThatThrownBy(() -> service.save(engineer, 3L, json(bad)))
                    .as(bad).isInstanceOf(ApiException.class);
        }
        verify(settings, never()).save(any());
    }

    @Test
    void whoCannotEditTheAnalysisCannotSaveItsBudget() {
        when(analyses.findManageable(engineer, 3L)).thenThrow(ApiException.notFound("El análisis no existe o no tienes acceso a él."));
        assertThatThrownBy(() -> service.save(engineer, 3L, json("{\"prices\":{}}"))).isInstanceOf(ApiException.class);
        verify(settings, never()).save(any());
    }

    @Test
    void readsEmptyGroupsWhenNothingWasSavedYet() {
        when(settings.findById(3L)).thenReturn(Optional.empty());
        JsonNode inputs = service.get(engineer, 3L);
        verify(analyses).findVisible(engineer, 3L);
        assertThat(inputs.get("prices").size()).isZero();
        assertThat(inputs.get("params").size()).isZero();
        assertThat(inputs.get("aiu").size()).isZero();
    }
}
