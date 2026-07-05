-- Demo import run for SAUDA-008 UI walkthrough (Distributor Supply A from V6).
-- Login: distributor-a@shop.kz / Sauda123!
-- After migrate: open /imports in distributor cabinet — run is awaiting approval with valid + error rows.

INSERT INTO raw_upload (
    id,
    distributor_id,
    uploaded_by_user_id,
    uploaded_by_role,
    original_filename,
    storage_path,
    file_size,
    mime_type,
    checksum,
    status
)
VALUES (
    '33333333-3333-3333-3333-333333333301',
    '11111111-1111-1111-1111-111111111104',
    '22222222-2222-2222-2222-222222222204',
    'distributor_manager',
    'demo_prices.csv',
    'raw/11111111-1111-1111-1111-111111111104/demo_prices.csv',
    512,
    'text/csv',
    'demo-checksum-008',
    'processed'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO import_run (
    id,
    distributor_id,
    raw_upload_id,
    adapter_key,
    status,
    rows_total,
    rows_ok,
    rows_err,
    total_rows,
    parsed_rows_count,
    error_rows_count,
    source_filename,
    started_at,
    finished_at
)
VALUES (
    '44444444-4444-4444-4444-444444444401',
    '11111111-1111-1111-1111-111111111104',
    '33333333-3333-3333-3333-333333333301',
    'csv_v1',
    'awaiting_approval',
    3,
    2,
    1,
    3,
    3,
    1,
    'demo_prices.csv',
    TIMESTAMPTZ '2026-07-01T10:00:00Z',
    TIMESTAMPTZ '2026-07-01T10:01:00Z'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO parsed_row (
    id,
    import_run_id,
    source_row_number,
    raw_row_data,
    parsed_data,
    status,
    errors,
    warnings
)
VALUES
    (
        '55555555-5555-5555-5555-555555555501',
        '44444444-4444-4444-4444-444444444401',
        2,
        '{"sku":"DEMO-SKU-001","name":"Samsung 990 PRO 1TB"}'::jsonb,
        '{"sku":"DEMO-SKU-001","name":"Samsung 990 PRO 1TB","brand":"Samsung","mpn":"990-1TB","price":125000,"price_includes_vat":true,"stock_quantity":15,"stock_status":"in_stock","lead_time_days":3}'::jsonb,
        'valid',
        NULL,
        NULL
    ),
    (
        '55555555-5555-5555-5555-555555555502',
        '44444444-4444-4444-4444-444444444401',
        3,
        '{"sku":"DEMO-SKU-002","name":"Kingston NV2 1TB"}'::jsonb,
        '{"sku":"DEMO-SKU-002","name":"Kingston NV2 1TB","brand":"Kingston","mpn":"NV2","price":45000,"stock_quantity":8,"stock_status":"in_stock","lead_time_days":2}'::jsonb,
        'needs_review',
        NULL,
        '[{"field":"price_includes_vat","code":"MISSING","message":"price_includes_vat is not set"}]'::jsonb
    ),
    (
        '55555555-5555-5555-5555-555555555503',
        '44444444-4444-4444-4444-444444444401',
        4,
        '{"sku":"DEMO-SKU-BAD","name":"Broken row"}'::jsonb,
        '{"sku":"DEMO-SKU-BAD","name":"Broken row","price":null}'::jsonb,
        'error',
        '[{"field":"price","code":"INVALID_NUMBER","message":"Invalid price"}]'::jsonb,
        NULL
    )
ON CONFLICT (id) DO NOTHING;
