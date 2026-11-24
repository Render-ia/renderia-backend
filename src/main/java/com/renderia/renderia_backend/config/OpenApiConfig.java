package com.renderia.renderia_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Documentación de la API con Swagger (springdoc-openapi).
 * - Pantalla interactiva: /swagger-ui.html
 * - Definición OpenAPI en JSON: /v3/api-docs
 * Para probar un endpoint protegido: iniciar sesión con POST /api/v1/auth/login (o /auth/demo),
 * copiar el token y pegarlo en el botón "Authorize".
 */
@Configuration
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI renderiaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Render.IA — API REST")
                        .version("v1")
                        .description("Backend de Render.IA: sube planos 2D, los analiza con el servicio de IA "
                                + "intercambiable y guarda los elementos estructurales para el modelo 3D. "
                                + "Ciclo completo en una llamada: POST /api/v1/analyses/run. "
                                + "Integrantes: Juan David Moreno y Felipe Cerón."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
