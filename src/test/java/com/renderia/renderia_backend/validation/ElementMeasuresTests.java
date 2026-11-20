package com.renderia.renderia_backend.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.StructuralElement;

class ElementMeasuresTests {

    @Test
    void appliesOnlyTheMeasuresSent() {
        StructuralElement element = new StructuralElement();
        element.setHeight(2.5);
        element.setThickness(0.15);

        ElementMeasures.apply(element, Map.of("height", 2.7));

        assertThat(element.getHeight()).isEqualTo(2.7);
        assertThat(element.getThickness()).isEqualTo(0.15);
    }

    @Test
    void acceptsTextWithDecimalCommaAndRoundsToTheMillimeter() {
        assertThat(ElementMeasures.parse("0,1234", "El espesor")).isEqualTo(0.123);
    }

    @Test
    void nullClearsTheMeasure() {
        StructuralElement element = new StructuralElement();
        element.setWidth(0.3);
        Map<String, Object> changes = new HashMap<>();
        changes.put("width", null);

        ElementMeasures.apply(element, changes);

        assertThat(element.getWidth()).isNull();
    }

    @Test
    void rejectsValuesOutOfRangeOrNotNumbers() {
        assertThatThrownBy(() -> ElementMeasures.parse(0, "La altura"))
                .isInstanceOf(ApiException.class).hasMessageContaining("entre 0,01 y 50");
        assertThatThrownBy(() -> ElementMeasures.parse(120, "La altura")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ElementMeasures.parse("alto", "La altura"))
                .isInstanceOf(ApiException.class).hasMessageContaining("número");
        assertThatThrownBy(() -> ElementMeasures.parse(Double.NaN, "La altura")).isInstanceOf(ApiException.class);
    }
}
