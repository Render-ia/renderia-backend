package com.renderia.renderia_backend.ai;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import com.renderia.renderia_backend.dto.AiServiceResponse;
import com.renderia.renderia_backend.exception.ApiException;

/**
 * Cliente HTTP del servicio de IA (repositorio renderia-ai). El backend no sabe
 * cómo se analiza el plano: solo le pasa la imagen, la escala y el modelo elegido.
 */
@Component
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private static final Pattern MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final RestClient client;
    private final String apiKey;

    public AiServiceClient(@Value("${app.ai.url}") String url, @Value("${app.ai.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);
        // En el plan gratis el servicio puede tardar en despertar.
        factory.setReadTimeout(120_000);
        this.client = RestClient.builder()
                .baseUrl(normalizeUrl(url))
                .requestFactory(factory)
                .build();
        // Quita espacios y saltos de línea que suelen colarse al pegar la clave.
        this.apiKey = apiKey == null ? "" : apiKey.replaceAll("\\s", "");
    }

    /** Acepta la URL con o sin https:// y sin espacios ni barra final. */
    static String normalizeUrl(String url) {
        String clean = url == null ? "" : url.replaceAll("\\s", "").replaceAll("/+$", "");
        if (clean.isEmpty()) {
            return "http://localhost:8000";
        }
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://" + clean;
        }
        return clean;
    }

    /** Intentos ante errores de la plataforma (servicio despertando o reiniciándose). */
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_WAIT_MS = 8_000;

    public AiServiceResponse analyze(String imageUrl, String scale, String provider, String modelIdentifier) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("imageUrl", imageUrl);
        body.put("scale", scale);
        body.put("provider", provider);
        body.put("modelIdentifier", modelIdentifier);

        for (int attempt = 1; ; attempt++) {
            try {
                return send(body);
            } catch (PlatformUnavailableException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                            "El servicio de IA no está disponible (código " + e.statusCode
                                    + "). Si estaba dormido, espera un minuto e inténtalo de nuevo.");
                }
                log.warn("El servicio de IA respondió {} sin mensaje; reintento {} de {}", e.statusCode, attempt + 1, MAX_ATTEMPTS);
                waitBeforeRetry();
            }
        }
    }

    private AiServiceResponse send(Map<String, Object> body) {
        try {
            AiServiceResponse response = client.post()
                    .uri("/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", apiKey)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, res) -> {
                        String text = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        int code = res.getStatusCode().value();
                        log.warn("El servicio de IA respondió {}: {}", code, abbreviate(text));
                        Matcher matcher = MESSAGE.matcher(text);
                        if (matcher.find()) {
                            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, matcher.group(1).replace("\\\"", "\""));
                        }
                        if (code == 502 || code == 503 || code == 504) {
                            throw new PlatformUnavailableException(code);
                        }
                        throw new ApiException(HttpStatus.BAD_GATEWAY,
                                "El servicio de IA no pudo analizar el plano (código " + code + ").");
                    })
                    .body(AiServiceResponse.class);
            if (response == null || response.elements() == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "El servicio de IA respondió sin elementos.");
            }
            return response;
        } catch (ResourceAccessException e) {
            log.warn("No se pudo conectar con el servicio de IA: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de IA no responde. Si estaba dormido, espera un minuto e inténtalo de nuevo.");
        } catch (RestClientException e) {
            if (e.getCause() instanceof ApiException apiException) {
                throw apiException;
            }
            if (e.getCause() instanceof PlatformUnavailableException unavailable) {
                throw unavailable;
            }
            log.error("Respuesta inesperada del servicio de IA", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "La respuesta del servicio de IA no se pudo leer: " + e.getMostSpecificCause().getMessage());
        }
    }

    private static void waitBeforeRetry() {
        try {
            Thread.sleep(RETRY_WAIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Se canceló la espera del servicio de IA.");
        }
    }

    private static String abbreviate(String text) {
        return text.length() > 300 ? text.substring(0, 300) + "…" : text;
    }

    /** Error de la plataforma de despliegue (no del análisis): vale la pena reintentar. */
    private static final class PlatformUnavailableException extends RuntimeException {
        private final int statusCode;

        PlatformUnavailableException(int statusCode) {
            super("El servicio de IA respondió " + statusCode);
            this.statusCode = statusCode;
        }
    }
}
