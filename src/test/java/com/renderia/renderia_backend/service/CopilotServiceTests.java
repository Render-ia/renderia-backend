package com.renderia.renderia_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.renderia.renderia_backend.ai.AiServiceClient;
import com.renderia.renderia_backend.dto.CopilotRequest;
import com.renderia.renderia_backend.dto.RenderRequest;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.AiAnalysis;
import com.renderia.renderia_backend.model.CopilotMessage;
import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.repository.CopilotMessageRepository;
import com.renderia.renderia_backend.repository.ElementTypeRepository;
import com.renderia.renderia_backend.repository.MaterialRepository;
import com.renderia.renderia_backend.repository.StructuralElementRepository;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.security.quota.DailyQuota;

class CopilotServiceTests {

    private AiServiceClient ai;
    private AnalysisService analyses;
    private CopilotService service;
    private CopilotMessageRepository messages;
    private final CurrentUser engineer = new CurrentUser(7L, Role.ENGINEER);

    @BeforeEach
    void setUp() {
        ai = mock(AiServiceClient.class);
        analyses = mock(AnalysisService.class);
        messages = mock(CopilotMessageRepository.class);
        DailyQuota quota = new DailyQuota(Clock.fixed(Instant.parse("2026-11-20T08:00:00Z"), ZoneOffset.UTC));
        service = new CopilotService(ai, analyses, mock(FloorPlanService.class), mock(ProjectService.class),
                mock(StructuralElementRepository.class), mock(ElementTypeRepository.class), mock(MaterialRepository.class),
                messages, quota, 60, 20, 1);
    }

    @Test
    void rejectsAnEmptyQuestionWithoutCallingTheAiService() {
        assertThatThrownBy(() -> service.ask(engineer, 1L, new CopilotRequest("  ", null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Escribe una pregunta");
        verify(ai, never()).postJson(anyString(), any());
    }

    @Test
    void checksAccessBeforeSpendingQuota() {
        when(analyses.findVisible(engineer, 9L)).thenThrow(ApiException.notFound("El análisis no existe o no tienes acceso a él."));

        assertThatThrownBy(() -> service.ask(engineer, 9L, new CopilotRequest("¿Qué muros son delgados?", null, null, null)))
                .hasMessageContaining("no tienes acceso");
        verify(ai, never()).postJson(anyString(), any());
    }

    @Test
    void validatesRenderStyleAndLimitsRendersPerDay() {
        when(analyses.findVisible(engineer, 1L)).thenReturn(new AiAnalysis());
        when(ai.postJson(eq("/render"), any())).thenReturn(Map.of("mimeType", "image/png"));

        assertThatThrownBy(() -> service.render(engineer, 1L, new RenderRequest("aGVsbG8=", "barroco", null, null)))
                .hasMessageContaining("estilo");

        service.render(engineer, 1L, new RenderRequest("aGVsbG8=", "modern", "exterior", null));
        assertThatThrownBy(() -> service.render(engineer, 1L, new RenderRequest("aGVsbG8=", "modern", "exterior", null)))
                .hasMessageContaining("límite diario");
    }

    @Test
    void aRenderTheAiCouldNotMakeDoesNotSpendTheDailyLimit() {
        when(analyses.findVisible(engineer, 1L)).thenReturn(new AiAnalysis());
        when(ai.postJson(eq("/render"), any()))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Gemini no responde"))
                .thenReturn(Map.of("mimeType", "image/png"));

        assertThatThrownBy(() -> service.render(engineer, 1L, new RenderRequest("aGVsbG8=", "modern", "exterior", null)))
                .hasMessageContaining("Gemini no responde");
        // The limit is 1 per day: the failed attempt was given back, so this one still works.
        service.render(engineer, 1L, new RenderRequest("aGVsbG8=", "modern", "exterior", null));
    }

    @Test
    void keepsOnlyTheLastMessagesOfTheConversation() {
        List<CopilotRequest.Message> messages = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            messages.add(new CopilotRequest.Message(i % 2 == 0 ? "user" : "system", "mensaje " + i));
        }
        messages.add(new CopilotRequest.Message("user", "   "));

        List<Map<String, String>> history = CopilotService.history(messages);

        assertThat(history).hasSize(CopilotService.MAX_HISTORY - 1);
        assertThat(history.get(0).get("text")).isEqualTo("mensaje 6");
        assertThat(history).allSatisfy(m -> assertThat(m.get("role")).isIn("user", "model"));
    }

    @Test
    void dropsQuantitiesThatAreNotNumbers() {
        List<Map<String, Object>> rows = CopilotService.quantities(List.of(
                new CopilotRequest.Quantity("Concreto en losa", "m³", 12.6, 13_900_000.0),
                new CopilotRequest.Quantity("Roto", "m", Double.NaN, null),
                new CopilotRequest.Quantity(null, "m", 1.0, null)));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("totalCop", 13_900_000.0);
    }

    @Test
    void savesTheQuestionAndTheAnswerWithItsProposals() {
        service.remember(engineer, 3L, "¿Qué muros son delgados?",
                Map.of("answer", "El muro #4 mide 8 cm.", "proposals", List.of(Map.of("elementId", 4, "field", "thickness", "value", 0.12))));

        ArgumentCaptor<CopilotMessage> saved = ArgumentCaptor.forClass(CopilotMessage.class);
        verify(messages, times(2)).save(saved.capture());
        CopilotMessage question = saved.getAllValues().get(0);
        CopilotMessage answer = saved.getAllValues().get(1);
        assertThat(question.getRole()).isEqualTo("user");
        assertThat(question.getContent()).isEqualTo("¿Qué muros son delgados?");
        assertThat(question.getUserId()).isEqualTo(7L);
        assertThat(answer.getRole()).isEqualTo("model");
        assertThat(answer.getAiAnalysisId()).isEqualTo(3L);
        assertThat(answer.getProposals()).contains("\"elementId\":4");
    }

    @Test
    void doesNotSaveAnAnswerWithoutText() {
        service.remember(engineer, 3L, "hola", Map.of("proposals", List.of()));
        verify(messages, never()).save(any());
    }

    @Test
    void returnsTheConversationOldestFirstAfterCheckingAccess() {
        when(messages.findTop100ByAiAnalysisIdAndUserIdOrderByIdDesc(3L, 7L)).thenReturn(List.of(
                new CopilotMessage(3L, 7L, "model", "respuesta", "[{\"elementId\":1}]"),
                new CopilotMessage(3L, 7L, "user", "pregunta", null)));

        List<Map<String, Object>> turns = service.conversation(engineer, 3L);

        verify(analyses).findVisible(engineer, 3L);
        assertThat(turns).extracting(t -> t.get("text")).containsExactly("pregunta", "respuesta");
        assertThat(turns.get(0).get("proposals")).isEqualTo(List.of());
        assertThat(turns.get(1).get("proposals").toString()).contains("elementId");
    }

    @Test
    void aConversationOfAnotherUsersAnalysisIsRefused() {
        when(analyses.findVisible(engineer, 9L)).thenThrow(ApiException.notFound("El análisis no existe o no tienes acceso a él."));
        assertThatThrownBy(() -> service.conversation(engineer, 9L)).isInstanceOf(ApiException.class);
        verify(messages, never()).findTop100ByAiAnalysisIdAndUserIdOrderByIdDesc(any(), any());
    }
}
