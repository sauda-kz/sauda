-- =====================================================================
-- SAUDA-067 — Phase 0: money, VAT flag, stock quantity/status
--
-- * Money stored as NUMERIC(18,2); never float; no implicit rounding on storage.
-- * price_includes_vat replaces vat_status enum (true / false / null).
-- * stock_qty renamed to stock_quantity; low_stock added to stock_status.
-- =====================================================================

-- ---------- Money columns → NUMERIC(18,2) ----------
ALTER TABLE offer
    ALTER COLUMN price TYPE NUMERIC(18,2);

ALTER TABLE price_history
    ALTER COLUMN price TYPE NUMERIC(18,2);

ALTER TABLE lot
    ALTER COLUMN budget_amount TYPE NUMERIC(18,2);

ALTER TABLE lot_match
    ALTER COLUMN estimated_unit_price TYPE NUMERIC(18,2),
    ALTER COLUMN estimated_total_price TYPE NUMERIC(18,2),
    ALTER COLUMN budget_amount TYPE NUMERIC(18,2),
    ALTER COLUMN estimated_margin TYPE NUMERIC(18,2);

ALTER TABLE spend_limit
    ALTER COLUMN amount TYPE NUMERIC(18,2);

ALTER TABLE cart_item
    ALTER COLUMN price_snapshot TYPE NUMERIC(18,2);

ALTER TABLE "order"
    ALTER COLUMN total TYPE NUMERIC(18,2),
    ALTER COLUMN vat_total TYPE NUMERIC(18,2);

ALTER TABLE order_item
    ALTER COLUMN unit_price_snapshot TYPE NUMERIC(18,2);

-- ---------- VAT: vat_status → price_includes_vat ----------
ALTER TABLE offer ADD COLUMN price_includes_vat BOOLEAN;

UPDATE offer SET price_includes_vat = CASE
    WHEN vat_status = 'with_vat' THEN TRUE
    WHEN vat_status = 'without_vat' THEN FALSE
    ELSE NULL
END;

ALTER TABLE offer DROP COLUMN vat_status;
DROP TYPE vat_status;

-- ---------- Stock ----------
ALTER TYPE stock_status ADD VALUE IF NOT EXISTS 'low_stock';

ALTER TABLE offer RENAME COLUMN stock_qty TO stock_quantity;
