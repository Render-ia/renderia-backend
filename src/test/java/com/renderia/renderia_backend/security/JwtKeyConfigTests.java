package com.renderia.renderia_backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtKeyConfigTests {

    private static final String STRONG = "una-clave-de-prueba-suficientemente-larga-1234567890";

    @Test
    void produccionSinClaveNoArranca() {
        assertThatThrownBy(() -> JwtKeyConfig.resolveSecret("", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Falta JWT_SECRET");
    }

    @Test
    void laClaveQueEstuvoEnGithubSeRechazaSiempre() {
        assertThatThrownBy(() -> JwtKeyConfig.resolveSecret("renderia-local-development-secret-change-me", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("público");
    }

    @Test
    void unaClaveCortaSeRechaza() {
        assertThatThrownBy(() -> JwtKeyConfig.resolveSecret("corta", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void enLocalSinClaveSeGeneraUnaAleatoriaDistintaCadaVez() {
        byte[] first = JwtKeyConfig.resolveSecret(null, false);
        byte[] second = JwtKeyConfig.resolveSecret(null, false);
        assertThat(first).hasSize(64);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void unaClaveFuerteSeUsaTalCual() {
        assertThat(JwtKeyConfig.resolveSecret("  " + STRONG + "\n", true)).isEqualTo(STRONG.getBytes());
    }
}
