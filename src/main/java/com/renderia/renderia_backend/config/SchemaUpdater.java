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
        """,
        // 002: modelo de visión local del servicio de IA. Solo la primera vez queda como predeterminado.
        """
        WITH inserted AS (
            INSERT INTO ai_models (name, provider, model_identifier, is_active, is_default)
            VALUES ('Detector de visión local', 'local-vision', 'renderia-vision-v1', TRUE, FALSE)
            ON CONFLICT (model_identifier) DO NOTHING
            RETURNING id
        )
        UPDATE ai_models SET is_default = (id IN (SELECT id FROM inserted))
        WHERE EXISTS (SELECT 1 FROM inserted)
        """,
        // 003: al borrar un análisis también se borra su modelo 3D.
        """
        ALTER TABLE models_3d
            DROP CONSTRAINT IF EXISTS models_3d_ai_analysis_id_fkey,
            ADD CONSTRAINT models_3d_ai_analysis_id_fkey
                FOREIGN KEY (ai_analysis_id) REFERENCES ai_analyses(id) ON DELETE CASCADE
        """,
        // 004: índices en las llaves foráneas que más se consultan.
        "CREATE INDEX IF NOT EXISTS idx_projects_user_id ON projects(user_id)",
        "CREATE INDEX IF NOT EXISTS idx_floor_plans_project_id ON floor_plans(project_id)",
        "CREATE INDEX IF NOT EXISTS idx_ai_analyses_floor_plan_id ON ai_analyses(floor_plan_id)",
        "CREATE INDEX IF NOT EXISTS idx_structural_elements_analysis_id ON structural_elements(ai_analysis_id)",
        "CREATE INDEX IF NOT EXISTS idx_models_3d_analysis_id ON models_3d(ai_analysis_id)",
        "CREATE INDEX IF NOT EXISTS idx_activity_logs_user_id ON activity_logs(user_id)",
        // 005: Reviewer and Client roles.
        """
        INSERT INTO roles (name, description) VALUES
            ('Revisor', 'Revisa y valida los proyectos de todos, sin borrar'),
            ('Cliente', 'Ve los proyectos que le comparten, en modo cliente')
        ON CONFLICT (name) DO NOTHING
        """,
        // 006: projects shared with other users (read access).
        """
        CREATE TABLE IF NOT EXISTS project_members (
            id         BIGSERIAL PRIMARY KEY,
            project_id BIGINT    NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
            user_id    BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            UNIQUE (project_id, user_id)
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_project_members_user_id ON project_members(user_id)",
        // 007: geometric detector checked by Gemini (falls back to the detector alone without a key).
        """
        INSERT INTO ai_models (name, provider, model_identifier, is_active, is_default)
        VALUES ('Detector de visión + Gemini', 'gemini', 'gemini:flash', TRUE, FALSE)
        ON CONFLICT (model_identifier) DO NOTHING
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
