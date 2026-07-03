-- Demo canonical products and offers for SAUDA-070 E2E matching (distributor A/B from V6).

INSERT INTO canonical_product (id, normalized_name, category, brand, model_mpn, mpn_norm, is_active)
VALUES
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001',
        'Samsung 990 PRO 1TB',
        'SSD',
        'Samsung',
        '990 PRO',
        '990pro',
        TRUE
    ),
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0002',
        'Kingston NV2 1TB',
        'SSD',
        'Kingston',
        'NV2',
        'nv2',
        TRUE
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO offer (
    id,
    distributor_id,
    canonical_product_id,
    internal_sku,
    raw_name,
    brand,
    model_mpn,
    price,
    currency,
    price_includes_vat,
    stock_quantity,
    stock_status,
    lead_time
)
VALUES
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0001',
        '11111111-1111-1111-1111-111111111104',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001',
        'SSD-SAM-990-1TB',
        'Samsung 990 PRO 1TB',
        'Samsung',
        '990 PRO',
        45000.00,
        'KZT',
        TRUE,
        120,
        'in_stock',
        '3 days'
    ),
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0002',
        '11111111-1111-1111-1111-111111111105',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0002',
        'SSD-KIN-NV2-1TB',
        'Kingston NV2 1TB',
        'Kingston',
        'NV2',
        38000.00,
        'KZT',
        TRUE,
        50,
        'in_stock',
        '5 days'
    )
ON CONFLICT (id) DO NOTHING;

-- Additional demo manager login (password: Sauda123!)
INSERT INTO app_user (id, organization_id, email, password_hash, is_active)
SELECT
    '22222222-2222-2222-2222-222222222206',
    '11111111-1111-1111-1111-111111111104',
    'distributor@sauda.kz',
    '$2a$10$uJVxtlnkf4NMuC3q5cQNZedtxXXpvX3MbOeJiVe/Exy8tl8h0tMo.',
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'distributor@sauda.kz');

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN app_role r ON r.code = 'distributor_manager'
WHERE u.email = 'distributor@sauda.kz'
  AND NOT EXISTS (
      SELECT 1 FROM app_user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
