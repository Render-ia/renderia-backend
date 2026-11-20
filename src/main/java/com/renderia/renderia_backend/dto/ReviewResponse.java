package com.renderia.renderia_backend.dto;

import java.time.Instant;

/** Who marked an analysis as reviewed and when, stamped by the server. */
public record ReviewResponse(Long analysisId, Long reviewerId, String reviewerName, Instant reviewedAt) {
}
