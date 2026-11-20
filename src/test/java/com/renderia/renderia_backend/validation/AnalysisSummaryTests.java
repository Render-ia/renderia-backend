package com.renderia.renderia_backend.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.renderia.renderia_backend.exception.ApiException;

class AnalysisSummaryTests {

    private static final String REVIEWED = "{\"warnings\":[],\"validation\":{\"reviewedBy\":\"Revisora real\",\"reviewedAt\":\"2026-11-20T10:00:00Z\"}}";

    @Test
    void theBrowserCannotForgeTheSignOff() {
        String result = AnalysisSummary.fromClient("{\"validation\":{\"reviewedBy\":\"Falso\"}}", null);
        assertFalse(result.contains("Falso"));
        assertFalse(result.contains("validation"));
    }

    @Test
    void keepsTheServerSignOffWhenTheBrowserRewritesTheSummary() {
        String result = AnalysisSummary.fromClient("{\"warnings\":[\"nuevo\"],\"validation\":{\"reviewedBy\":\"Falso\"}}", REVIEWED);
        assertTrue(result.contains("\"reviewedBy\":\"Revisora real\""));
        assertTrue(result.contains("nuevo"));
        assertFalse(result.contains("Falso"));
    }

    @Test
    void keepsTheSignOffEvenIfTheBrowserLeavesItOut() {
        assertTrue(AnalysisSummary.fromClient("{\"warnings\":[]}", REVIEWED).contains("Revisora real"));
    }

    @Test
    void rejectsTextThatIsNotAJsonObject() {
        for (String bad : new String[] {"no es json", "[1,2]", "\"texto\"", "42", "null", "{"}) {
            ApiException e = assertThrows(ApiException.class, () -> AnalysisSummary.fromClient(bad, null), bad);
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    void rejectsAHugeSummary() {
        String huge = "{\"a\":\"" + "x".repeat(AnalysisSummary.MAX_CHARS) + "\"}";
        assertThrows(ApiException.class, () -> AnalysisSummary.fromClient(huge, null));
    }

    @Test
    void ignoresABrokenStoredSummary() {
        assertEquals("{\"a\":1}", AnalysisSummary.fromClient("{\"a\":1}", "roto"));
    }
}
