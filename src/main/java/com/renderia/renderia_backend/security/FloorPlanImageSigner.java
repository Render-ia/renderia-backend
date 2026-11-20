package com.renderia.renderia_backend.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Firma los enlaces de las imágenes de los planos.
 *
 * La imagen tiene que poder abrirse sin token (va en una etiqueta img y el servicio
 * de IA la descarga), pero no debe bastar con adivinar el id. Por eso cada enlace
 * lleva una fecha de vencimiento y una firma HMAC-SHA256 de "id:vencimiento".
 *
 * El vencimiento se redondea al final del día siguiente: durante un mismo día el
 * enlace no cambia y el navegador puede guardarlo en caché, y nunca vive más de 48 horas.
 */
@Component
public class FloorPlanImageSigner {

    private static final long DAY_SECONDS = Duration.ofDays(1).toSeconds();

    private final byte[] signingKey;
    private final String publicUrl;
    private final Clock clock;

    @Autowired
    public FloorPlanImageSigner(SecretKey jwtSigningKey, @Value("${app.public-url}") String publicUrl) {
        this(jwtSigningKey, publicUrl, Clock.systemUTC());
    }

    FloorPlanImageSigner(SecretKey jwtSigningKey, String publicUrl, Clock clock) {
        // Clave derivada: si se filtra una firma de imagen no sirve para falsificar tokens.
        this.signingKey = hmac(jwtSigningKey.getEncoded(), "renderia/floor-plan-images/v1");
        this.publicUrl = publicUrl.replaceAll("/+$", "");
        this.clock = clock;
    }

    /** Enlace público y firmado de la imagen de un plano. */
    public String signedUrl(Long floorPlanId) {
        long now = clock.instant().getEpochSecond();
        long expires = (now / DAY_SECONDS + 2) * DAY_SECONDS;
        return publicUrl + "/api/v1/floor-plans/" + floorPlanId + "/image?exp=" + expires
                + "&sig=" + signature(floorPlanId, expires);
    }

    /** true si la firma corresponde a ese plano y todavía no venció. */
    public boolean isValid(Long floorPlanId, Long expires, String sig) {
        if (floorPlanId == null || expires == null || sig == null || sig.isBlank()) {
            return false;
        }
        if (clock.instant().getEpochSecond() > expires) {
            return false;
        }
        byte[] expected = signature(floorPlanId, expires).getBytes(StandardCharsets.US_ASCII);
        byte[] given = sig.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, given);
    }

    /** Segundos que faltan para que venza el enlace (para la cabecera Cache-Control). */
    public long secondsUntil(long expires) {
        return Math.max(0, expires - clock.instant().getEpochSecond());
    }

    private String signature(Long floorPlanId, long expires) {
        byte[] mac = hmac(signingKey, floorPlanId + ":" + expires);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac);
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 no está disponible", e);
        }
    }
}
