-- Sauda baseline schema
CREATE TABLE IF NOT EXISTS schema_version_marker (
    id BIGSERIAL PRIMARY KEY,
    note VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO schema_version_marker (note) VALUES ('Sauda MVP foundation baseline');
