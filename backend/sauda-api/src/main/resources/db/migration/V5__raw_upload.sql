-- =====================================================================
-- SAUDA-068: Raw distributor file uploads (price lists / stock exports)
-- After schema changes: update docs/er-diagram.md
-- =====================================================================

CREATE TYPE raw_upload_status AS ENUM ('uploaded', 'processing', 'processed', 'failed');

CREATE TABLE raw_upload (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    distributor_id      UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    uploaded_by_user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    uploaded_by_role      VARCHAR(64) NOT NULL,
    original_filename   TEXT NOT NULL,
    storage_path        TEXT NOT NULL,
    file_size           BIGINT NOT NULL,
    mime_type           VARCHAR(255) NOT NULL,
    checksum            VARCHAR(64) NOT NULL,
    status              raw_upload_status NOT NULL DEFAULT 'uploaded',
    error_message       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_raw_upload_distributor ON raw_upload (distributor_id, created_at DESC);

-- platform_admin: allow backup upload scenario (import:run)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN permission p ON p.code = 'import:run'
WHERE r.code = 'platform_admin';
