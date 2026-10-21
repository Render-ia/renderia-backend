package com.renderia.renderia_backend.dto;

public record Model3DRequest(Long floorPlanId, Long aiAnalysisId, String fileUrl, String format, Integer elementCount) {
}
