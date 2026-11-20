package com.renderia.renderia_backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class FloorPlanImageSignerTests {

    private static final SecretKeySpec KEY = new SecretKeySpec(
            "clave-de-prueba-de-32-bytes-o-mas-123456".getBytes(), "HmacSHA256");
    private static final Pattern PARAMS = Pattern.compile("exp=(\\d+)&sig=([A-Za-z0-9_-]+)$");
    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private static FloorPlanImageSigner signerAt(Instant instant) {
        return new FloorPlanImageSigner(KEY, "https://api.example.com/", Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static Matcher params(String url) {
        Matcher m = PARAMS.matcher(url);
        assertThat(m.find()).isTrue();
        return m;
    }

    @Test
    void elEnlaceFirmadoSeAceptaParaSuPlano() {
        FloorPlanImageSigner signer = signerAt(NOW);
        String url = signer.signedUrl(7L);
        assertThat(url).startsWith("https://api.example.com/api/v1/floor-plans/7/image?exp=");
        Matcher m = params(url);
        assertThat(signer.isValid(7L, Long.valueOf(m.group(1)), m.group(2))).isTrue();
    }

    @Test
    void laFirmaDeUnPlanoNoAbreOtro() {
        FloorPlanImageSigner signer = signerAt(NOW);
        Matcher m = params(signer.signedUrl(7L));
        assertThat(signer.isValid(8L, Long.valueOf(m.group(1)), m.group(2))).isFalse();
    }

    @Test
    void cambiarElVencimientoInvalidaLaFirma() {
        FloorPlanImageSigner signer = signerAt(NOW);
        Matcher m = params(signer.signedUrl(7L));
        assertThat(signer.isValid(7L, Long.valueOf(m.group(1)) + 86_400, m.group(2))).isFalse();
    }

    @Test
    void elEnlaceVenceEnMenosDe48Horas() {
        Matcher m = params(signerAt(NOW).signedUrl(7L));
        long exp = Long.parseLong(m.group(1));
        assertThat(signerAt(NOW.plusSeconds(47 * 3600)).isValid(7L, exp, m.group(2))).isFalse();
        assertThat(signerAt(NOW.plusSeconds(3600)).isValid(7L, exp, m.group(2))).isTrue();
    }

    @Test
    void sinFirmaNoHayImagen() {
        assertThat(signerAt(NOW).isValid(7L, null, null)).isFalse();
        assertThat(signerAt(NOW).isValid(7L, NOW.getEpochSecond() + 100, "")).isFalse();
    }

    @Test
    void elEnlaceNoCambiaDuranteElMismoDia() {
        assertThat(signerAt(NOW).signedUrl(7L)).isEqualTo(signerAt(NOW.plusSeconds(3600)).signedUrl(7L));
    }
}
