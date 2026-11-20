package com.renderia.renderia_backend.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ImageSignatureTests {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};
    private static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] SVG = "<?xml version=\"1.0\"?>\n<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"
            .getBytes(StandardCharsets.UTF_8);

    @Test
    void acceptsRealImagesOfEachAllowedType() {
        assertTrue(ImageSignature.matches("image/png", PNG));
        assertTrue(ImageSignature.matches("image/jpeg", JPEG));
        assertTrue(ImageSignature.matches("image/webp", WEBP));
        assertTrue(ImageSignature.matches("image/svg+xml", SVG));
    }

    @Test
    void rejectsContentThatDoesNotMatchItsDeclaredType() {
        assertFalse(ImageSignature.matches("image/png", JPEG));
        assertFalse(ImageSignature.matches("image/jpeg", PNG));
        assertFalse(ImageSignature.matches("image/svg+xml", PNG));
    }

    @Test
    void rejectsTextRenamedAsImage() {
        byte[] text = "esto no es un plano, es un archivo de texto".getBytes(StandardCharsets.UTF_8);
        assertFalse(ImageSignature.matches("image/png", text));
        assertFalse(ImageSignature.matches("image/svg+xml", text));
    }

    @Test
    void rejectsEmptyFilesAndUnknownTypes() {
        assertFalse(ImageSignature.matches("image/png", new byte[0]));
        assertFalse(ImageSignature.matches("application/pdf", PNG));
        assertFalse(ImageSignature.matches(null, PNG));
    }
}
