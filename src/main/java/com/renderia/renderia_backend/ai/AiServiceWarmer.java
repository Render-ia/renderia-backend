package com.renderia.renderia_backend.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Mantiene despierto el servicio de IA mientras el backend está despierto: al
 * arrancar y cada 10 minutos le consulta /health. Así, cuando alguien usa la
 * aplicación, la IA ya está lista y el análisis no choca con el arranque en frío.
 * Cuando nadie usa el backend, este también se duerme y deja de consultar.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.ai.keep-warm", havingValue = "true", matchIfMissing = true)
public class AiServiceWarmer {

    private final AiServiceClient aiServiceClient;

    public AiServiceWarmer(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    @Scheduled(initialDelayString = "PT20S", fixedDelayString = "PT10M")
    public void keepWarm() {
        aiServiceClient.ping();
    }
}
