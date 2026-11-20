package com.renderia.renderia_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.renderia.renderia_backend.exception.ApiException;

class AnalysisElementLimitsTests {

    @Test
    void acceptsNormalCoordinates() {
        assertEquals(12.5, AnalysisService.coordinate(12.5));
        assertEquals(-3.0, AnalysisService.coordinate(-3.0));
        assertNull(AnalysisService.coordinate(null));
    }

    @Test
    void rejectsCoordinatesOutsideAnyPlan() {
        assertThrows(ApiException.class, () -> AnalysisService.coordinate(1e308));
        assertThrows(ApiException.class, () -> AnalysisService.coordinate(Double.NaN));
        assertThrows(ApiException.class, () -> AnalysisService.coordinate(Double.NEGATIVE_INFINITY));
        assertThrows(ApiException.class, () -> AnalysisService.coordinate(-200_000.0));
    }

    @Test
    void zeroOrMissingMeasureMeansUnknown() {
        assertNull(AnalysisService.detectedMeasure(null, "La altura"));
        assertNull(AnalysisService.detectedMeasure(0.0, "La altura"));
    }

    @Test
    void tinyMeasuresRoundUpToOneCentimeter() {
        assertEquals(0.01, AnalysisService.detectedMeasure(0.001, "El espesor"));
    }

    @Test
    void rejectsNegativeOrAbsurdMeasures() {
        assertThrows(ApiException.class, () -> AnalysisService.detectedMeasure(-5.0, "La altura"));
        assertThrows(ApiException.class, () -> AnalysisService.detectedMeasure(1e9, "La altura"));
        assertThrows(ApiException.class, () -> AnalysisService.detectedMeasure(Double.POSITIVE_INFINITY, "La altura"));
    }

    @Test
    void keepsRealMeasuresToTheMillimeter() {
        assertEquals(2.5, AnalysisService.detectedMeasure(2.5, "La altura"));
        assertEquals(0.153, AnalysisService.detectedMeasure(0.15321, "El espesor"));
    }
}
