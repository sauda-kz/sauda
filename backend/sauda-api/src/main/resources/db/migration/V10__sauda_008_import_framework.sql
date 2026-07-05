-- =====================================================================
-- SAUDA-008: Import framework — extended import_run, parsed_row,
-- status enum extensions, offer import metadata, import:approve permission.
--
-- NOTE (PostgreSQL): values added via ALTER TYPE ... ADD VALUE must NOT be
-- referenced as literals within this same migration/transaction. Therefore
-- import_run.status keeps its existing column default; the application layer
-- sets the initial status ('pending') explicitly on insert.
--
-- After schema changes: update docs/er-diagram.md and docs/auth-rbac.md (step 11).
-- =====================================================================

-- ---------- import_run status extensions ----------
-- Legacy values (running/success/partial/failed) stay for backward compat.
-- 'failed' already exists from V2.
ALTER TYPE import_status ADD VALUE IF NOT EXISTS 'pending';
ALTER TYPE import_status ADD VALUE IF NOT EXISTS 'processing';
ALTER TYPE import_status ADD VALUE IF NOT EXISTS 'parsed';
ALTER TYPE import_status ADD VALUE IF NOT EXISTS 'parsed_with_errors';
ALTER TYPE import_status ADD VALUE IF NOT EXISTS 'awaiting_approval';
ALTER TYPE import_status ADD VALUE IF NOT EXISTS 'approved';
ALTER TYPE import_status ADD VALUE IF NOT EXISTS 'rejected';
ALTER TYPE import_status ADD VALUE IF NOT EXISTS 'applied';

-- ---------- import_run: SAUDA-008 columns ----------
ALTER TABLE import_run
    ADD COLUMN IF NOT EXISTS raw_upload_id       UUID REFERENCES raw_upload(id) ON DELETE RESTRICT,
    ADD COLUMN IF NOT EXISTS adapter_key         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS total_rows          INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS parsed_rows_count   INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS error_rows_count    INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS approved_by_user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS approved_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejected_by_user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS rejected_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at          TIMESTAMPTZ NOT NULL DEFAULT now();

-- started_at is now set when processing begins (status transitions out of pending),
-- not at row creation.
ALTER TABLE import_run ALTER COLUMN started_at DROP NOT NULL;
ALTER TABLE import_run ALTER COLUMN started_at DROP DEFAULT;

CREATE INDEX IF NOT EXISTS ix_import_run_raw_upload ON import_run (raw_upload_id);
CREATE INDEX IF NOT EXISTS ix_import_run_status ON import_run (distributor_id, status);
CREATE INDEX IF NOT EXISTS ix_import_run_distributor_created
    ON import_run (distributor_id, created_at DESC);

-- ---------- parsed_row: draft rows between raw file and offer ----------
CREATE TYPE parsed_row_status AS ENUM ('valid', 'needs_review', 'error', 'edited');

CREATE TABLE parsed_row (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_run_id      UUID NOT NULL REFERENCES import_run(id) ON DELETE CASCADE,
    source_row_number  INTEGER,
    raw_row_data       JSONB NOT NULL DEFAULT '{}'::jsonb,
    parsed_data        JSONB NOT NULL DEFAULT '{}'::jsonb,
    status             parsed_row_status NOT NULL,
    errors             JSONB,
    warnings           JSONB,
    edited_by_user_id  UUID REFERENCES app_user(id) ON DELETE SET NULL,
    edited_at          TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_parsed_row_run_status ON parsed_row (import_run_id, status);
CREATE INDEX ix_parsed_row_run_number ON parsed_row (import_run_id, source_row_number);

-- ---------- offer: import metadata ----------
-- offer.source_file_id (FK -> import_run) already exists and is reused as
-- source_import_run_id. offer.internal_sku is reused as supplier_sku.
-- offer.price_includes_vat (boolean, since V5) is populated directly.
ALTER TABLE offer ADD COLUMN IF NOT EXISTS last_imported_at TIMESTAMPTZ;

-- ---------- permission: import:approve ----------
INSERT INTO permission (code, resource, action, description)
VALUES ('import:approve', 'import', 'approve',
        'Approve/reject an import and apply parsed rows to offers')
ON CONFLICT (code) DO NOTHING;

-- Granted only to distributor_manager. platform_admin is read-only for imports
-- and must not approve on behalf of the distributor.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN permission p ON p.code = 'import:approve'
WHERE r.code = 'distributor_manager'
ON CONFLICT DO NOTHING;
