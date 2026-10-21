package com.renderia.renderia_backend.model;

import java.util.Set;

/**
 * Estados de un análisis y los cambios permitidos entre ellos:
 * PENDING → PROCESSING → COMPLETED o FAILED, y de FAILED se puede reintentar (PENDING).
 *
 * Patrón State: cada estado sabe a cuáles puede pasar, así ningún servicio
 * tiene que repetir esas reglas con if.
 */
public enum AnalysisStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    public Set<AnalysisStatus> next() {
        return switch (this) {
            case PENDING -> Set.of(PROCESSING);
            case PROCESSING -> Set.of(COMPLETED, FAILED);
            case COMPLETED -> Set.of();
            case FAILED -> Set.of(PENDING);
        };
    }

    public boolean canMoveTo(AnalysisStatus target) {
        return next().contains(target);
    }
}
