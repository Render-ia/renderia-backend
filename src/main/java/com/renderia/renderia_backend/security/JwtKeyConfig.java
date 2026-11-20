package com.renderia.renderia_backend.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** Crea el codificador y el decodificador de tokens JWT firmados con HS256. */
@Configuration
public class JwtKeyConfig {

    /** Valor por defecto de app.jwt.secret en application.properties, solo para trabajar en local. */
    static final String DEVELOPMENT_SECRET = "renderia-local-development-secret-change-me";

    private final SecretKey key;

    public JwtKeyConfig(@Value("${app.jwt.secret}") String secret) {
        // En Render (variable RENDER) nunca se arranca con la clave de desarrollo del repositorio.
        if (System.getenv("RENDER") != null && secret.equals(DEVELOPMENT_SECRET)) {
            throw new IllegalStateException("Define JWT_SECRET en Render: no se puede usar la clave de desarrollo.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres.");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
