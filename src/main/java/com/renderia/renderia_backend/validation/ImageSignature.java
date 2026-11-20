package com.renderia.renderia_backend.validation;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Revisa que el contenido de un archivo corresponda de verdad al tipo de imagen
 * que dice ser, mirando sus primeros bytes ("firma" o "número mágico"). Así un
 * archivo cualquiera renombrado a .png no se acepta como plano.
 */
public final class ImageSignature {

    private static final int SVG_SCAN_BYTES = 2048;

    private ImageSignature() {
    }

    public static boolean matches(String contentType, byte[] bytes) {
        if (contentType == null || bytes == null || bytes.length < 4) {
            return false;
        }
        return switch (contentType) {
            case "image/png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
            case "image/webp" -> bytes.length >= 12
                    && startsWith(bytes, 'R', 'I', 'F', 'F')
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            case "image/svg+xml" -> looksLikeSvg(bytes);
            default -> false;
        };
    }

    private static boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeSvg(byte[] bytes) {
        int length = Math.min(bytes.length, SVG_SCAN_BYTES);
        String head = new String(bytes, 0, length, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return head.contains("<svg");
    }
}
