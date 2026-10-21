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
                .baseUrl(url.replaceAll("/+$", ""))
                .requestFactory(factory)
                .build();
        this.apiKey = apiKey;
    }

    public AiServiceResponse analyze(String imageUrl, String scale, String provider, String modelIdentifier) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("imageUrl", imageUrl);
        body.put("scale", scale);
        body.put("provider", provider);
        body.put("modelIdentifier", modelIdentifier);

        try {
            AiServiceResponse response = client.post()
                    .uri("/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", apiKey)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, res) -> {
                        String text = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.warn("El servicio de IA respondió {}: {}", res.getStatusCode(), text);
                        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, messageOf(text));
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
            log.error("Respuesta inesperada del servicio de IA", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "La respuesta del servicio de IA no se pudo leer: " + e.getMostSpecificCause().getMessage());
        }
    }

    private static String messageOf(String body) {
        Matcher matcher = MESSAGE.matcher(body);
        if (matcher.find()) {
            return matcher.group(1).replace("\\\"", "\"");
        }
        return "El servicio de IA no pudo analizar el plano.";
    }
}
