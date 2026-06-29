-- =====================================================================
-- Sauda — initial schema (Flyway migration V2)
-- Full target model: identity/orgs + RBAC, product catalog (supply-side,
-- Phase 0), lots & matching, buyer flow (cart/order), data-ingestion ops.
--
-- Notes:
--   * Money stored as NUMERIC(14,2) in minor-unit-safe precision; never float.
--   * Organization-based isolation enforced in app + via FKs to organization.
--   * RBAC: app_role / permission / role_permission / app_user_role (many-to-many).
--   * After schema changes: update docs/er-diagram.md (Mermaid ER diagram).
--   * Buyer block (cart/order/...) is created now but not exercised in Phase 0.
-- =====================================================================

-- ---------- Extensions ----------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "citext";     -- case-insensitive email

-- =====================================================================
-- ENUM types
-- =====================================================================
CREATE TYPE organization_type AS ENUM ('platform', 'distributor', 'buyer');

CREATE TYPE vat_status AS ENUM ('with_vat', 'without_vat', 'unknown');

CREATE TYPE stock_status AS ENUM ('in_stock', 'out_of_stock', 'on_order', 'unknown');

CREATE TYPE lot_status AS ENUM ('active', 'expired', 'archived');

CREATE TYPE lot_match_status AS ENUM (
    'suggested', 'matched', 'needs_review', 'dismissed', 'mismatch_reported'
);

CREATE TYPE check_result AS ENUM ('ok', 'fail', 'unknown');

CREATE TYPE cart_status AS ENUM ('open', 'ordered', 'abandoned');

CREATE TYPE order_status AS ENUM (
    'draft', 'pending_approval', 'approved', 'rejected', 'fulfilled', 'cancelled'
);

CREATE TYPE import_status AS ENUM ('running', 'success', 'partial', 'failed');

-- =====================================================================
-- Identity & organizations  (SAUDA-005)
-- =====================================================================
CREATE TABLE organization (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type        organization_type NOT NULL,
    name        TEXT NOT NULL,
    bin         VARCHAR(12),                       -- KZ business id number
    vat_payer   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
    email            CITEXT,
    password_hash    TEXT NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_app_user_email ON app_user (email);
CREATE INDEX ix_app_user_org ON app_user (organization_id);

-- =====================================================================
-- RBAC  (scalable role / permission model)
-- =====================================================================
CREATE TABLE app_role (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code               TEXT NOT NULL,
    organization_type  organization_type NOT NULL,
    name               TEXT NOT NULL,
    description        TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_role_code UNIQUE (code)
);

CREATE TABLE permission (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        TEXT NOT NULL,
    resource    TEXT NOT NULL,
    action      TEXT NOT NULL,
    description TEXT,
    CONSTRAINT uq_permission_code UNIQUE (code)
);

CREATE TABLE role_permission (
    role_id       UUID NOT NULL REFERENCES app_role(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE app_user_role (
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id     UUID NOT NULL REFERENCES app_role(id) ON DELETE RESTRICT,
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX ix_app_user_role_role ON app_user_role (role_id);

-- Role must match the user's organization type (platform / distributor / buyer).
CREATE OR REPLACE FUNCTION check_user_role_org_type()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM app_user u
        JOIN organization o ON o.id = u.organization_id
        JOIN app_role r ON r.id = NEW.role_id
        WHERE u.id = NEW.user_id
          AND r.organization_type = o.type
    ) THEN
        RAISE EXCEPTION 'role organization_type does not match user organization type';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_app_user_role_org_type
    BEFORE INSERT OR UPDATE ON app_user_role
    FOR EACH ROW
    EXECUTE FUNCTION check_user_role_org_type();

-- ---------- RBAC seed: roles ----------
INSERT INTO app_role (code, organization_type, name, description) VALUES
    ('platform_admin',      'platform',    'Platform Admin',       'Platform operations: catalog, lots, matching oversight'),
    ('buyer',               'buyer',       'Buyer',                'Browse offers, manage cart and create orders'),
    ('buyer_approver',      'buyer',       'Buyer Approver',       'Approve purchase orders within spend limits'),
    ('distributor_manager', 'distributor', 'Distributor Manager',  'Manage offers, imports and lot matches'),
    ('distributor_viewer',  'distributor', 'Distributor Viewer',   'Read-only access to distributor data');

-- ---------- RBAC seed: permissions ----------
INSERT INTO permission (code, resource, action, description) VALUES
    ('org:read',                  'organization',     'read',   'View organization profile'),
    ('org:manage',                'organization',     'manage', 'Manage organization settings'),
    ('canonical_product:read',    'canonical_product','read',   'View canonical product catalog'),
    ('canonical_product:manage',  'canonical_product','manage', 'Create and update canonical products'),
    ('offer:read',                'offer',            'read',   'View offers / price list positions'),
    ('offer:manage',              'offer',            'manage', 'Create and update offers'),
    ('import:run',                'import',           'run',    'Upload and run price list imports'),
    ('import:read',               'import',           'read',   'View import run history and errors'),
    ('lot:read',                  'lot',              'read',   'View procurement lots'),
    ('lot:create',                'lot',              'create', 'Create procurement lots'),
    ('lot:manage',                'lot',              'manage', 'Update and archive lots'),
    ('lot_match:read',            'lot_match',        'read',   'View lot-to-offer matches'),
    ('lot_match:review',          'lot_match',        'review', 'Review and update match status'),
    ('lot_match:manage',          'lot_match',        'manage', 'Full control over lot matches'),
    ('cart:read',                 'cart',             'read',   'View shopping carts'),
    ('cart:manage',               'cart',             'manage', 'Create and update carts'),
    ('order:read',                'order',            'read',   'View purchase orders'),
    ('order:create',              'order',            'create', 'Create and submit purchase orders'),
    ('order:approve',             'order',            'approve','Approve or reject purchase orders'),
    ('cost_center:read',          'cost_center',      'read',   'View cost centers'),
    ('cost_center:manage',        'cost_center',      'manage', 'Manage cost centers'),
    ('spend_limit:read',          'spend_limit',      'read',   'View spend limits'),
    ('spend_limit:manage',        'spend_limit',      'manage', 'Manage spend limits');

-- ---------- RBAC seed: role → permission mappings ----------
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN permission p ON p.code IN (
    'org:read', 'org:manage',
    'canonical_product:read', 'canonical_product:manage',
    'offer:read',
    'import:read',
    'lot:read', 'lot:create', 'lot:manage',
    'lot_match:read', 'lot_match:review', 'lot_match:manage'
)
WHERE r.code = 'platform_admin';

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN permission p ON p.code IN (
    'org:read',
    'offer:read',
    'cart:read', 'cart:manage',
    'order:read', 'order:create',
    'cost_center:read'
)
WHERE r.code = 'buyer';

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN permission p ON p.code IN (
    'org:read',
    'offer:read',
    'cart:read', 'cart:manage',
    'order:read', 'order:create', 'order:approve',
    'cost_center:read',
    'spend_limit:read'
)
WHERE r.code = 'buyer_approver';

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN permission p ON p.code IN (
    'org:read',
    'offer:read', 'offer:manage',
    'import:run', 'import:read',
    'lot_match:read', 'lot_match:review'
)
WHERE r.code = 'distributor_manager';

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN permission p ON p.code IN (
    'org:read',
    'offer:read',
    'import:read',
    'lot_match:read'
)
WHERE r.code = 'distributor_viewer';

-- =====================================================================
-- Product catalog  (SAUDA-004)
-- =====================================================================
CREATE TABLE canonical_product (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    normalized_name  TEXT NOT NULL,
    category         TEXT,
    brand            TEXT,
    model_mpn        TEXT,
    mpn_norm         TEXT,
    attributes       JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_canonical_mpn_norm ON canonical_product (mpn_norm);
CREATE INDEX ix_canonical_brand_model ON canonical_product (brand, model_mpn);

CREATE TABLE offer (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    distributor_id       UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    canonical_product_id UUID REFERENCES canonical_product(id) ON DELETE SET NULL,
    internal_sku         TEXT,
    raw_name             TEXT NOT NULL,
    brand                TEXT,
    model_mpn            TEXT,
    price                NUMERIC(14,2),
    currency             CHAR(3) NOT NULL DEFAULT 'KZT',
    vat_status           vat_status NOT NULL DEFAULT 'unknown',
    stock_qty            INTEGER,
    stock_status         stock_status NOT NULL DEFAULT 'unknown',
    lead_time            TEXT,
    last_updated_at      TIMESTAMPTZ,
    source_file_id       UUID,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_offer_distributor ON offer (distributor_id);
CREATE INDEX ix_offer_canonical ON offer (canonical_product_id);
CREATE INDEX ix_offer_sku ON offer (distributor_id, internal_sku);

CREATE TABLE price_history (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offer_id      UUID NOT NULL REFERENCES offer(id) ON DELETE CASCADE,
    price         NUMERIC(14,2),
    currency      CHAR(3) NOT NULL DEFAULT 'KZT',
    stock_status  stock_status NOT NULL DEFAULT 'unknown',
    captured_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_price_history_offer ON price_history (offer_id, captured_at DESC);

-- =====================================================================
-- Lots & matching  (SAUDA-004 — core scenario)
-- =====================================================================
CREATE TABLE lot (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source             TEXT,
    external_lot_id    TEXT,
    title              TEXT,
    customer_name      TEXT,
    category           TEXT,
    description        TEXT,
    requirements_text  TEXT,
    quantity           INTEGER,
    budget_amount      NUMERIC(14,2),
    currency           CHAR(3) DEFAULT 'KZT',
    deadline_at        TIMESTAMPTZ,
    published_at       TIMESTAMPTZ,
    status             lot_status NOT NULL DEFAULT 'active',
    source_url         TEXT,
    raw_data           JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by         UUID REFERENCES app_user(id) ON DELETE SET NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_lot_status ON lot (status);
CREATE INDEX ix_lot_deadline ON lot (deadline_at);

CREATE TABLE lot_match (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id              UUID NOT NULL REFERENCES lot(id) ON DELETE CASCADE,
    offer_id            UUID NOT NULL REFERENCES offer(id) ON DELETE CASCADE,
    distributor_id      UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    match_status        lot_match_status NOT NULL DEFAULT 'suggested',
    confidence_score    NUMERIC(5,4),
    match_reason        TEXT,
    quantity_check      check_result NOT NULL DEFAULT 'unknown',
    stock_check         check_result NOT NULL DEFAULT 'unknown',
    price_check         check_result NOT NULL DEFAULT 'unknown',
    estimated_margin    NUMERIC(14,2),
    needs_manual_review BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lot_offer UNIQUE (lot_id, offer_id)
);
CREATE INDEX ix_lot_match_lot ON lot_match (lot_id);
CREATE INDEX ix_lot_match_offer ON lot_match (offer_id);
CREATE INDEX ix_lot_match_distributor ON lot_match (distributor_id);
CREATE INDEX ix_lot_match_status ON lot_match (match_status);

-- =====================================================================
-- Data ingestion / operations
-- =====================================================================
CREATE TABLE import_run (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    distributor_id  UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at     TIMESTAMPTZ,
    rows_total      INTEGER NOT NULL DEFAULT 0,
    rows_ok         INTEGER NOT NULL DEFAULT 0,
    rows_err        INTEGER NOT NULL DEFAULT 0,
    status          import_status NOT NULL DEFAULT 'running',
    source_filename TEXT
);
CREATE INDEX ix_import_run_distributor ON import_run (distributor_id, started_at DESC);

CREATE TABLE import_error (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_run_id  UUID NOT NULL REFERENCES import_run(id) ON DELETE CASCADE,
    source_row     INTEGER,
    reason         TEXT NOT NULL,
    raw            JSONB
);
CREATE INDEX ix_import_error_run ON import_error (import_run_id);

ALTER TABLE offer
    ADD CONSTRAINT fk_offer_source_file
    FOREIGN KEY (source_file_id) REFERENCES import_run(id) ON DELETE SET NULL;

-- =====================================================================
-- Buyer flow  (created now, NOT exercised in Phase 0)
-- =====================================================================
CREATE TABLE cost_center (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_company_id   UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    name               TEXT NOT NULL
);
CREATE INDEX ix_cost_center_buyer ON cost_center (buyer_company_id);

CREATE TABLE spend_limit (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_company_id   UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    user_id            UUID REFERENCES app_user(id) ON DELETE CASCADE,
    role_id            UUID REFERENCES app_role(id) ON DELETE CASCADE,
    period             TEXT,
    amount             NUMERIC(14,2) NOT NULL,
    CONSTRAINT chk_spend_limit_scope CHECK (NOT (user_id IS NOT NULL AND role_id IS NOT NULL))
);
CREATE INDEX ix_spend_limit_buyer ON spend_limit (buyer_company_id);
CREATE INDEX ix_spend_limit_role ON spend_limit (role_id);

CREATE TABLE cart (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_company_id   UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    created_by         UUID REFERENCES app_user(id) ON DELETE SET NULL,
    status             cart_status NOT NULL DEFAULT 'open',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_cart_buyer ON cart (buyer_company_id);

CREATE TABLE cart_item (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id         UUID NOT NULL REFERENCES cart(id) ON DELETE CASCADE,
    offer_id        UUID NOT NULL REFERENCES offer(id) ON DELETE RESTRICT,
    qty             INTEGER NOT NULL CHECK (qty > 0),
    price_snapshot  NUMERIC(14,2) NOT NULL
);
CREATE INDEX ix_cart_item_cart ON cart_item (cart_id);

CREATE TABLE "order" (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_company_id    UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
    created_by          UUID REFERENCES app_user(id) ON DELETE SET NULL,
    cost_center_id      UUID REFERENCES cost_center(id) ON DELETE SET NULL,
    status              order_status NOT NULL DEFAULT 'draft',
    total               NUMERIC(14,2) NOT NULL DEFAULT 0,
    vat_total           NUMERIC(14,2) NOT NULL DEFAULT 0,
    approved_by         UUID REFERENCES app_user(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_order_buyer ON "order" (buyer_company_id);

CREATE TABLE order_item (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id             UUID NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    offer_id             UUID NOT NULL REFERENCES offer(id) ON DELETE RESTRICT,
    canonical_product_id UUID REFERENCES canonical_product(id) ON DELETE SET NULL,
    qty                  INTEGER NOT NULL CHECK (qty > 0),
    unit_price_snapshot  NUMERIC(14,2) NOT NULL,
    vat_rate             NUMERIC(5,2)
);
CREATE INDEX ix_order_item_order ON order_item (order_id);

CREATE TABLE order_event (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    from_status  order_status,
    to_status    order_status NOT NULL,
    actor        UUID REFERENCES app_user(id) ON DELETE SET NULL,
    note         TEXT,
    at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_order_event_order ON order_event (order_id, at);
