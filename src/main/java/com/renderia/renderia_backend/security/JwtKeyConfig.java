package com.renderia.renderia_backend.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Crea el codificador y el decodificador de tokens JWT firmados con HS256.
 *
 * Reglas de la clave (JWT_SECRET):
 * - En producción (Render define RENDER=true) es obligatoria y debe tener 32 caracteres o más.
 *   Si falta, el backend no arranca: es preferible caerse a firmar tokens con una clave conocida.
 * - En local, si no se define, se genera una clave aleatoria en memoria. Las sesiones se
 *   pierden al reiniciar, pero nunca se usa una clave publicada en GitHub.
 */
@Configuration
public class JwtKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfig.class);
    static final int MIN_SECRET_BYTES = 32;

    /** Claves que alguna vez estuvieron escritas en el repositorio: nunca se aceptan. */
    private static final Set<String> KNOWN_PUBLIC_SECRETS = Set.of(
            "renderia-local-development-secret-change-me");

    private final SecretKey key;

    public JwtKeyConfig(@Value("${app.jwt.secret:}") String secret,
                        @Value("${app.production:false}") boolean production) {
        this.key = new SecretKeySpec(resolveSecret(secret, production), "HmacSHA256");
    }

    static byte[] resolveSecret(String secret, boolean production) {
        String clean = secret == null ? "" : secret.strip();
        if (KNOWN_PUBLIC_SECRETS.contains(clean)) {
            throw new IllegalStateException(
                    "JWT_SECRET usa un valor que es público en GitHub. Define una clave nueva y larga.");
        }
        if (clean.isEmpty()) {
            if (production) {
                throw new IllegalStateException(
                        "Falta JWT_SECRET. En producción es obligatoria (mínimo " + MIN_SECRET_BYTES + " caracteres).");
            }
            log.warn("JWT_SECRET no está definida: se usa una clave aleatoria temporal. "
                    + "Las sesiones se cerrarán al reiniciar el backend.");
            byte[] random = new byte[64];
            new SecureRandom().nextBytes(random);
            return random;
        }
        byte[] bytes = clean.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos " + MIN_SECRET_BYTES + " caracteres.");
        }
        return bytes;
    }

    /** Clave HMAC compartida con el firmador de enlaces de imágenes. */
    @Bean
    public SecretKey jwtSigningKey() {
        return key;
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
