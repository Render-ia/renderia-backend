package com.renderia.renderia_backend.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ReviewServiceTests {

    private static final Instant WHEN = Instant.parse("2026-11-20T15:00:00Z");

    @Test
    void stampsTheReviewKeepingTheRestOfTheSummary() {
        String result = ReviewService.withValidation("{\"warnings\":[\"a\"]}", "Marcela Ortiz", WHEN);
        assertTrue(result.contains("\"warnings\":[\"a\"]"));
        assertTrue(result.contains("\"reviewedBy\":\"Marcela Ortiz\""));
        assertTrue(result.contains("\"reviewedAt\":\"2026-11-20T15:00:00Z\""));
    }

    @Test
    void survivesAnEmptyOrBrokenSummary() {
        assertTrue(ReviewService.withValidation(null, "R", WHEN).contains("\"validation\""));
        assertTrue(ReviewService.withValidation("no es json", "R", WHEN).contains("\"validation\""));
    }
}
