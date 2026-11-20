package com.renderia.renderia_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renderia.renderia_backend.model.CopilotMessage;

public interface CopilotMessageRepository extends JpaRepository<CopilotMessage, Long> {

    /** The newest turns first; the service puts them back in order. */
    List<CopilotMessage> findTop100ByAiAnalysisIdAndUserIdOrderByIdDesc(Long aiAnalysisId, Long userId);
}
