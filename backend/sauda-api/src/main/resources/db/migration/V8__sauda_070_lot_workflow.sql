-- =====================================================================
-- SAUDA-070: Admin lot workflow — statuses, send timestamp, notifications,
-- lot document attachments.
-- After schema changes: update docs/er-diagram.md and docs/lot-mvp-structure.md.
-- =====================================================================

-- ---------- Lot status extensions ----------
ALTER TYPE lot_status ADD VALUE IF NOT EXISTS 'draft';
ALTER TYPE lot_status ADD VALUE IF NOT EXISTS 'needs_review';

-- ---------- lot_match: distributor send timestamp ----------
ALTER TABLE lot_match
    ADD COLUMN IF NOT EXISTS sent_to_distributor_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS ix_lot_match_sent
    ON lot_match (distributor_id, sent_to_distributor_at DESC)
    WHERE sent_to_distributor_at IS NOT NULL;

-- ---------- Internal in-app notifications ----------
CREATE TYPE notification_status AS ENUM ('unread', 'read');

CREATE TABLE internal_notification (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    lot_match_id   UUID NOT NULL REFERENCES lot_match(id) ON DELETE RESTRICT,
    title          TEXT NOT NULL,
    message        TEXT NOT NULL,
    status         notification_status NOT NULL DEFAULT 'unread',
    read_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_notification_read_at
        CHECK (
            (status = 'read' AND read_at IS NOT NULL)
            OR (status = 'unread' AND read_at IS NULL)
        )
);

CREATE INDEX ix_internal_notification_user_status
    ON internal_notification (user_id, status, created_at DESC);

CREATE INDEX ix_internal_notification_lot_match
    ON internal_notification (lot_match_id);

-- ---------- Lot document attachments (procurement files) ----------
CREATE TABLE lot_attachment (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id              UUID NOT NULL REFERENCES lot(id) ON DELETE RESTRICT,
    uploaded_by_user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    original_filename   TEXT NOT NULL,
    storage_path        TEXT NOT NULL,
    file_size           BIGINT NOT NULL,
    mime_type           VARCHAR(255) NOT NULL,
    checksum            VARCHAR(64) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_lot_attachment_lot
    ON lot_attachment (lot_id, created_at DESC);

-- ---------- RBAC: in-app notifications ----------
INSERT INTO permission (code, resource, action, description) VALUES
    ('notification:read', 'notification', 'read', 'View and mark in-app notifications as read')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r
JOIN permission p ON p.code = 'notification:read'
WHERE r.code IN ('distributor_manager', 'platform_admin')
ON CONFLICT DO NOTHING;
