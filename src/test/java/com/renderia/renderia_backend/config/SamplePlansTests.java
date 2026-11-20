package com.renderia.renderia_backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.renderia.renderia_backend.validation.ImageSignature;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** The sample plans and their stored analyses ship with the backend and must stay readable. */
class SamplePlansTests {

    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final String[] PLANS = {"house-floor-plan", "apartment-building-floor-plan", "school-floor-plan",
        "health-center-floor-plan", "retail-store-floor-plan", "warehouse-floor-plan"};
    private static final String[] ANALYZED = {"house-floor-plan", "apartment-building-floor-plan", "school-floor-plan",
        "health-center-floor-plan", "warehouse-floor-plan"};

    private static byte[] read(String file) throws IOException {
        try (InputStream input = new ClassPathResource("samples/" + file).getInputStream()) {
            return input.readAllBytes();
        }
    }

    @Test
    void everySamplePlanIsARealSvgImage() throws IOException {
        for (String plan : PLANS) {
            assertTrue(ImageSignature.matches("image/svg+xml", read(plan + ".svg")), plan);
        }
    }

    @Test
    void storedAnalysesHaveWallsColumnsAndASlab() throws IOException {
        for (String plan : ANALYZED) {
            JsonNode stored = JSON.readTree(read(plan + ".analysis.json"));
            Set<String> types = new HashSet<>();
            stored.get("elements").forEach(element -> types.add(element.path("type").asString()));
            assertTrue(types.containsAll(Set.of("Muro", "Columna", "Losa")), plan + " " + types);
            assertTrue(stored.get("summary").has("buildingSizeM"), plan);
        }
    }

    @Test
    void eachSampleIsADifferentBuilding() throws IOException {
        Set<String> sizes = new HashSet<>();
        for (String plan : ANALYZED) {
            sizes.add(JSON.readTree(read(plan + ".analysis.json")).get("summary").get("buildingSizeM").toString());
        }
        assertEquals(ANALYZED.length, sizes.size());
    }
}
