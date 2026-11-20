package com.renderia.renderia_backend.dto;

import java.util.List;

/**
 * Question for the engineering copilot about one analysis. The project data is
 * read from the database by the backend; the browser only sends the question,
 * the recent conversation and the quantities it computed.
 */
public record CopilotRequest(String question, String depth, List<Message> history, List<Quantity> quantities) {

    public record Message(String role, String text) {
    }

    public record Quantity(String item, String unit, Double quantity, Double total) {
    }
}
