package com.renderia.renderia_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Aplica al arrancar los cambios de esquema que se agregaron después de la primera versión
 * (ver renderia-database/migrations). Cada sentencia se puede repetir sin dañar nada.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaUpdater implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpdater.class);

    private static final String[] MIGRATIONS = {
        """
        CREATE TABLE IF NOT EXISTS floor_plan_files (
            floor_plan_id BIGINT      PRIMARY KEY REFERENCES floor_plans(id) ON DELETE CASCADE,
            content_type  VARCHAR(50) NOT NULL,
            data          BYTEA       NOT NULL
        )
        """
    };

    private final JdbcTemplate jdbc;

    public SchemaUpdater(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String sql : MIGRATIONS) {
            try {
                jdbc.execute(sql);
            } catch (RuntimeException e) {
                log.warn("No se pudo aplicar una migración del esquema: {}", e.getMessage());
            }
        }
    }
}
