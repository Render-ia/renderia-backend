package com.renderia.renderia_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Imagen original de un plano (PNG, JPG, WEBP o SVG). */
@Entity
@Table(name = "floor_plan_files")
public class FloorPlanFile {

    @Id
    @Column(name = "floor_plan_id")
    private Long floorPlanId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] data;

    protected FloorPlanFile() {
    }

    public FloorPlanFile(Long floorPlanId, String contentType, byte[] data) {
        this.floorPlanId = floorPlanId;
        this.contentType = contentType;
        this.data = data;
    }

    public Long getFloorPlanId() {
        return floorPlanId;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }
}
