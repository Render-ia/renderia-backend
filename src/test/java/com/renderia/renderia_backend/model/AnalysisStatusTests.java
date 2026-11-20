package com.renderia.renderia_backend.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Patrón State: solo se permiten las transiciones del ciclo de vida de un análisis. */
class AnalysisStatusTests {

    @Test
    void followsTheNormalLifecycle() {
        assertTrue(AnalysisStatus.PENDING.canMoveTo(AnalysisStatus.PROCESSING));
        assertTrue(AnalysisStatus.PROCESSING.canMoveTo(AnalysisStatus.COMPLETED));
        assertTrue(AnalysisStatus.PROCESSING.canMoveTo(AnalysisStatus.FAILED));
    }

    @Test
    void failedAnalysisCanBeRetried() {
        assertTrue(AnalysisStatus.FAILED.canMoveTo(AnalysisStatus.PENDING));
    }

    @Test
    void completedAnalysisIsFinal() {
        for (AnalysisStatus target : AnalysisStatus.values()) {
            assertFalse(AnalysisStatus.COMPLETED.canMoveTo(target));
        }
    }

    @Test
    void cannotSkipProcessing() {
        assertFalse(AnalysisStatus.PENDING.canMoveTo(AnalysisStatus.COMPLETED));
        assertFalse(AnalysisStatus.PENDING.canMoveTo(AnalysisStatus.FAILED));
    }
}
