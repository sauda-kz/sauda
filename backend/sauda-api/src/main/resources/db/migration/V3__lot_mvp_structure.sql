-- =====================================================================
-- Sauda — lot & lot_match MVP structure (SAUDA-006A)
-- Extends SAUDA-004 schema with structured procurement fields and match
-- metadata for distributor "Подходящие лоты".
-- After schema changes: update docs/er-diagram.md and docs/lot-mvp-structure.md.
-- =====================================================================

-- ---------- Enum extensions ----------
ALTER TYPE lot_status ADD VALUE IF NOT EXISTS 'cancelled';
ALTER TYPE lot_status ADD VALUE IF NOT EXISTS 'closed';

ALTER TYPE lot_match_status ADD VALUE IF NOT EXISTS 'not_matched';
ALTER TYPE lot_match_status ADD VALUE IF NOT EXISTS 'interested';

-- ---------- lot: rename legacy columns ----------
ALTER TABLE lot RENAME COLUMN deadline_at TO delivery_deadline;
ALTER TABLE lot RENAME COLUMN requirements_text TO technical_requirements;

ALTER INDEX ix_lot_deadline RENAME TO ix_lot_delivery_deadline;

-- ---------- lot: new columns ----------
ALTER TABLE lot
    ADD COLUMN external_purchase_id       TEXT,
    ADD COLUMN procurement_method         TEXT,
    ADD COLUMN lot_type                   TEXT,
    ADD COLUMN unit                       TEXT,
    ADD COLUMN delivery_location          TEXT,
    ADD COLUMN submission_deadline        TIMESTAMPTZ,
    ADD COLUMN warranty_requirements      TEXT,
    ADD COLUMN required_documents         TEXT,
    ADD COLUMN qualification_requirements TEXT,
    ADD COLUMN contract_terms_summary     TEXT,
    ADD COLUMN raw_text                   TEXT,
    ADD COLUMN updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now();

-- Auto-update updated_at on row change
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_lot_updated_at
    BEFORE UPDATE ON lot
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ---------- lot_match: new columns ----------
ALTER TABLE lot_match
    ADD COLUMN matched_requirements  JSONB,
    ADD COLUMN missing_requirements  JSONB,
    ADD COLUMN risk_flags            JSONB,
    ADD COLUMN required_quantity     INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN available_quantity    INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN estimated_unit_price  NUMERIC(14,2),
    ADD COLUMN estimated_total_price NUMERIC(14,2),
    ADD COLUMN budget_amount         NUMERIC(14,2),
    ADD COLUMN admin_comment         TEXT NOT NULL DEFAULT '',
    ADD COLUMN distributor_comment   TEXT;
