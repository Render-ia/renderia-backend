package com.renderia.renderia_backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** One turn of a user's conversation with the copilot about an analysis. */
@Entity
@Table(name = "copilot_messages")
public class CopilotMessage {

    public static final String USER = "user";
    public static final String MODEL = "model";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ai_analysis_id", nullable = false)
    private Long aiAnalysisId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Changes the copilot proposed with this answer, as a JSON array (null for questions). */
    @Column(columnDefinition = "TEXT")
    private String proposals;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CopilotMessage() {
    }

    public CopilotMessage(Long aiAnalysisId, Long userId, String role, String content, String proposals) {
        this.aiAnalysisId = aiAnalysisId;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.proposals = proposals;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAiAnalysisId() {
        return aiAnalysisId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getProposals() {
        return proposals;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
