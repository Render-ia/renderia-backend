package com.renderia.renderia_backend.dto;

/** AI render of a capture of the 3D viewer (base64 PNG or JPEG). */
public record RenderRequest(String image, String style, String scene, String notes) {
}
