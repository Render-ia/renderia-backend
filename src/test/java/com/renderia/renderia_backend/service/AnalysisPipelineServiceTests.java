package com.renderia.renderia_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.renderia.renderia_backend.ai.AiServiceClient;
import com.renderia.renderia_backend.dto.AiServiceResponse;
import com.renderia.renderia_backend.dto.AnalysisResponse;
import com.renderia.renderia_backend.dto.DetectedElement;
import com.renderia.renderia_backend.dto.ElementInput;
import com.renderia.renderia_backend.dto.Model3DRequest;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.AiAnalysis;
import com.renderia.renderia_backend.model.AiModel;
import com.renderia.renderia_backend.model.AnalysisStatus;
import com.renderia.renderia_backend.model.ElementType;
import com.renderia.renderia_backend.model.FloorPlan;
import com.renderia.renderia_backend.model.Material;
import com.renderia.renderia_backend.repository.AiAnalysisRepository;
import com.renderia.renderia_backend.repository.ElementTypeRepository;
import com.renderia.renderia_backend.repository.MaterialRepository;
import com.renderia.renderia_backend.security.CurrentUser;

/** The full cycle run by the backend: PENDING → PROCESSING → COMPLETED or FAILED. */
class AnalysisPipelineServiceTests {

    private static final long ANALYSIS = 7L;
    private static final long PLAN = 3L;
    private static final long MODEL = 2L;
    private static final long WALL = 1L;
    private static final long BRICK = 5L;

    private final CurrentUser engineer = new CurrentUser(1L, "Ingeniero");

    private AnalysisService analysisService;
    private AiAnalysisRepository analyses;
    private Model3DService models3d;
    private AiServiceClient ai;
    private ActivityLogService activity;
    private AnalysisPipelineService pipeline;
    private AiAnalysis analysis;

    @BeforeEach
    void setUp() {
        analysisService = mock(AnalysisService.class);
        analyses = mock(AiAnalysisRepository.class);
        FloorPlanService floorPlans = mock(FloorPlanService.class);
        models3d = mock(Model3DService.class);
        ElementTypeRepository elementTypes = mock(ElementTypeRepository.class);
        MaterialRepository materials = mock(MaterialRepository.class);
        ai = mock(AiServiceClient.class);
        activity = mock(ActivityLogService.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        analysis = new AiAnalysis();
        ReflectionTestUtils.setField(analysis, "id", ANALYSIS);
        analysis.setFloorPlanId(PLAN);
        analysis.setAiModelId(MODEL);

        FloorPlan plan = new FloorPlan();
        ReflectionTestUtils.setField(plan, "id", PLAN);
        ReflectionTestUtils.setField(plan, "floorLevel", 1);
        ReflectionTestUtils.setField(plan, "scale", "1:100");

        AiModel model = new AiModel();
        ReflectionTestUtils.setField(model, "id", MODEL);
        ReflectionTestUtils.setField(model, "name", "Detector local");
        ReflectionTestUtils.setField(model, "provider", "local-vision");
        ReflectionTestUtils.setField(model, "modelIdentifier", "opencv");
        model.setActive(true);

        ElementType wall = new ElementType();
        ReflectionTestUtils.setField(wall, "id", WALL);
        wall.setName("Muro");
        Material brick = new Material();
        ReflectionTestUtils.setField(brick, "id", BRICK);
        brick.setName("Ladrillo");

        when(analysisService.findManageable(engineer, ANALYSIS)).thenReturn(analysis);
        when(analysisService.requireActiveModel(MODEL)).thenReturn(model);
        when(floorPlans.findManageable(engineer, PLAN)).thenReturn(plan);
        when(floorPlans.imageUrl(plan)).thenReturn("https://renderia-backend.onrender.com/api/v1/floor-plans/3/image");
        when(analyses.findById(ANALYSIS)).thenReturn(Optional.of(analysis));
        when(analyses.save(any(AiAnalysis.class))).thenAnswer(call -> call.getArgument(0));
        when(elementTypes.findAll()).thenReturn(List.of(wall));
        when(materials.findAll()).thenReturn(List.of(brick));

        pipeline = new AnalysisPipelineService(analysisService, analyses, floorPlans, models3d,
                elementTypes, materials, ai, activity, transactions);
    }

    private static DetectedElement wall(Double confidence) {
        return new DetectedElement("Muro", "Ladrillo", 0.0, 0.0, 4.0, 0.0, null, 2.5, 0.15, confidence);
    }

    @Test
    void completedRunSavesElementsAndThe3dModel() {
        when(ai.analyze(anyString(), eq("1:100"), eq("local-vision"), eq("opencv"))).thenReturn(new AiServiceResponse(
                "local-vision", "opencv", 80L,
                List.of(wall(0.9), wall(1.7), new DetectedElement("Nave espacial", null, 1.0, 1.0, null, null, null, null, null, 0.5)),
                Map.of("rooms", 2, "validation", "falsa")));

        AnalysisResponse result = pipeline.run(engineer, ANALYSIS, null);

        assertEquals("COMPLETED", result.status());
        assertNull(result.errorMessage());
        assertTrue(result.rawResponse().contains("\"orchestratedBy\":\"backend\""));
        assertTrue(!result.rawResponse().contains("falsa"), "the browser-free summary cannot carry a sign-off");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ElementInput>> rows = ArgumentCaptor.forClass(List.class);
        verify(analysisService).saveElements(eq(engineer), eq(ANALYSIS), rows.capture());
        assertEquals(2, rows.getValue().size(), "unknown element types are skipped");
        assertEquals(WALL, rows.getValue().get(0).elementTypeId());
        assertEquals(BRICK, rows.getValue().get(0).materialId());
        assertEquals(1.0, rows.getValue().get(1).confidence(), "confidence is kept between 0 and 1");

        ArgumentCaptor<Model3DRequest> model = ArgumentCaptor.forClass(Model3DRequest.class);
        verify(models3d).create(eq(engineer), model.capture());
        assertEquals(2, model.getValue().elementCount());
        verify(activity).record(eq(1L), eq("Análisis completado"), anyString());
    }

    @Test
    void aiFailureLeavesTheAnalysisFailedInsteadOfProcessing() {
        when(ai.analyze(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de IA no responde."));

        AnalysisResponse result = pipeline.run(engineer, ANALYSIS, "1:50");

        assertEquals("FAILED", result.status());
        assertEquals("El servicio de IA no responde.", result.errorMessage());
        verify(analysisService, never()).saveElements(any(), any(), anyList());
        verify(activity).record(eq(1L), eq("Análisis fallido"), anyString());
    }

    @Test
    void internalErrorsAreNotShownToTheUser() {
        when(ai.analyze(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("org.postgresql.util.PSQLException: secreto"));

        AnalysisResponse result = pipeline.run(engineer, ANALYSIS, null);

        assertEquals("FAILED", result.status());
        assertTrue(!result.errorMessage().contains("PSQL"));
    }

    @Test
    void failedAnalysisCanBeRetried() {
        analysis.setStatus(AnalysisStatus.PROCESSING);
        analysis.setStatus(AnalysisStatus.FAILED);
        analysis.setErrorMessage("antes falló");
        when(ai.analyze(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new AiServiceResponse("local-vision", "opencv", 50L, List.of(wall(0.8)), null));

        assertEquals("COMPLETED", pipeline.run(engineer, ANALYSIS, null).status());
    }

    @Test
    void completedAnalysisCannotRunAgain() {
        analysis.setStatus(AnalysisStatus.COMPLETED);

        ApiException error = assertThrows(ApiException.class, () -> pipeline.run(engineer, ANALYSIS, null));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(ai, never()).analyze(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void rowsIgnoreElementsWithoutPosition() {
        List<ElementInput> rows = AnalysisPipelineService.toRows(
                List.of(new DetectedElement("Muro", "Desconocido", null, 1.0, null, null, null, null, null, null)),
                Map.of("Muro", WALL), Map.of());
        assertTrue(rows.isEmpty());
        assertTrue(AnalysisPipelineService.toRows(null, Map.of(), Map.of()).isEmpty());
    }
}
