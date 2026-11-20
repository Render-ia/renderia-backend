package com.renderia.renderia_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.renderia.renderia_backend.exception.ApiException;

class Model3DServiceTests {

    @Test
    void acceptsWebLinksOrNoLink() {
        assertEquals("https://cdn.renderia.co/m.glb", Model3DService.fileUrl("https://cdn.renderia.co/m.glb"));
        assertNull(Model3DService.fileUrl(null));
        assertNull(Model3DService.fileUrl("  "));
    }

    @Test
    void rejectsLinksThatRunCode() {
        for (String bad : new String[] {"javascript:alert(1)", "JavaScript:alert(1)", "data:text/html,<script>", "file:///etc/passwd", "//evil.example"}) {
            assertThrows(ApiException.class, () -> Model3DService.fileUrl(bad), bad);
        }
        assertThrows(ApiException.class, () -> Model3DService.fileUrl("https://" + "a".repeat(600)));
    }

    @Test
    void normalizesKnownFormatsAndRejectsTheRest() {
        assertEquals("GLB", Model3DService.format(null));
        assertEquals("GLTF", Model3DService.format(" gltf "));
        assertThrows(ApiException.class, () -> Model3DService.format("G".repeat(5000)));
        assertThrows(ApiException.class, () -> Model3DService.format("exe"));
    }
}
