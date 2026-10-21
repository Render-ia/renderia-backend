package com.renderia.renderia_backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AiServiceClientTests {

    @Test
    void normalizaLaUrlDelServicioDeIa() {
        assertEquals("https://renderia-ai.onrender.com", AiServiceClient.normalizeUrl(" renderia-ai.onrender.com/\n"));
        assertEquals("https://renderia-ai.onrender.com", AiServiceClient.normalizeUrl("https://renderia-ai.onrender.com/"));
        assertEquals("http://localhost:8000", AiServiceClient.normalizeUrl("http://localhost:8000"));
        assertEquals("http://localhost:8000", AiServiceClient.normalizeUrl("  "));
    }
}
