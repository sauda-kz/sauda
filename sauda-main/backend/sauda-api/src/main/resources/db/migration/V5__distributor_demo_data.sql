-- Demo distributor org, user, catalog seed and lot matches for frontend dev (SAUDA-005+006)
-- Password for dist@technodist.kz: Sauda123! (same BCrypt hash as V4)

INSERT INTO organization (id, type, name, bin, vat_payer) VALUES
    ('11111111-1111-1111-1111-111111111104', 'distributor', 'ТОО «ТехноДист»', '987654321098', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO app_user (id, organization_id, email, password_hash, is_active) VALUES
    (
        '22222222-2222-2222-2222-222222222204',
        '11111111-1111-1111-1111-111111111104',
        'dist@technodist.kz',
        '$2a$10$uJVxtlnkf4NMuC3q5cQNZedtxXXpvX3MbOeJiVe/Exy8tl8h0tMo.',
        TRUE
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN app_role r ON r.code = 'distributor_manager'
WHERE u.email = 'dist@technodist.kz'
ON CONFLICT DO NOTHING;

-- Canonical products
INSERT INTO canonical_product (id, normalized_name, category, brand, model_mpn, mpn_norm) VALUES
    ('33333333-3333-3333-3333-333333333301', 'Hikvision DS-2CD2043G2-I', 'видеонаблюдение', 'Hikvision', 'DS-2CD2043G2-I', 'DS2CD2043G2I'),
    ('33333333-3333-3333-3333-333333333302', 'Mikrotik hAP ac2', 'сетевое оборудование', 'Mikrotik', 'RBD52G-5HacD2HnD-TC', 'RBD52G5HACD2HNDTC'),
    ('33333333-3333-3333-3333-333333333303', 'HP ProLiant DL380 Gen10', 'серверное оборудование', 'HP', 'DL380 Gen10', 'DL380GEN10')
ON CONFLICT (id) DO NOTHING;

-- Offers for TechnoDist
INSERT INTO offer (
    id, distributor_id, canonical_product_id, internal_sku, raw_name, brand, model_mpn,
    price, currency, vat_status, stock_qty, stock_status, lead_time
) VALUES
    (
        '44444444-4444-4444-4444-444444444401',
        '11111111-1111-1111-1111-111111111104',
        '33333333-3333-3333-3333-333333333301',
        'HKV-DS2CD2043G2',
        'Hikvision DS-2CD2043G2-I',
        'Hikvision',
        'DS-2CD2043G2-I',
        28500, 'KZT', 'included', 120, 'in_stock', '3 рабочих дня'
    ),
    (
        '44444444-4444-4444-4444-444444444402',
        '11111111-1111-1111-1111-111111111104',
        '33333333-3333-3333-3333-333333333302',
        'MT-RBD52G',
        'Mikrotik hAP ac2 RBD52G',
        'Mikrotik',
        'RBD52G-5HacD2HnD-TC',
        38990, 'KZT', 'included', 45, 'in_stock', '1–2 дня'
    ),
    (
        '44444444-4444-4444-4444-444444444403',
        '11111111-1111-1111-1111-111111111104',
        '33333333-3333-3333-3333-333333333303',
        'HP-DL380G10',
        'HP ProLiant DL380 Gen10 Server',
        'HP',
        'DL380 Gen10',
        980000, 'KZT', 'included', 8, 'low', '10–12 рабочих дней'
    )
ON CONFLICT (id) DO NOTHING;

-- Lots (procurement)
INSERT INTO lot (
    id, source, external_lot_id, title, customer_name, category, description,
    quantity, unit, budget_amount, currency, delivery_location,
    delivery_deadline, submission_deadline, technical_requirements, required_documents,
    status, source_url, lot_type
) VALUES
    (
        '55555555-5555-5555-5555-555555555501',
        'samruk',
        'LOT-2026-1042',
        'IP-камеры видеонаблюдения',
        'АО «КазМунайГаз»',
        'видеонаблюдение',
        'Закупка IP-камер для объекта в Атырау',
        50, 'шт', 4200000, 'KZT', 'г. Атырау, пр. Азаттык 6',
        now() + interval '3 days',
        now() + interval '23 days',
        '4 МП, PoE, уличное исполнение, IK10',
        'Сертификат дистрибьютора, Декларация ТР ТС, Гарантийный талон',
        'active',
        'https://goszakup.gov.kz/ru/announce/index/example/1042',
        'товар'
    ),
    (
        '55555555-5555-5555-5555-555555555502',
        'goszakup',
        'LOT-2026-0881',
        'Коммутаторы доступа Cisco',
        'ТОО «Qazaq Telecom»',
        'сетевое оборудование',
        'Коммутаторы для филиальной сети',
        12, 'шт', 8900000, 'KZT', 'г. Алматы',
        now() + interval '14 days',
        now() + interval '5 days',
        'L2/L3, PoE+, минимум 24 порта',
        'Сертификат авторизованного партнёра Cisco',
        'active',
        'https://goszakup.gov.kz/ru/announce/index/example/0881',
        'товар'
    ),
    (
        '55555555-5555-5555-5555-555555555503',
        'commercial',
        'LOT-2026-0312',
        'Серверное оборудование HP',
        'ТехноПарк ТОО',
        'серверное оборудование',
        'Серверы для ЦОД',
        2, 'шт', 2100000, 'KZT', 'г. Алматы',
        now() + interval '20 days',
        now() + interval '12 days',
        '2U, redundant PSU, iLO',
        'Счёт, коммерческое предложение',
        'active',
        'https://example.kz/lot/0312',
        'товар'
    )
ON CONFLICT (id) DO NOTHING;

-- Lot matches
INSERT INTO lot_match (
    id, lot_id, offer_id, distributor_id, match_status, confidence_score, match_reason,
    matched_requirements, missing_requirements, risk_flags,
    required_quantity, available_quantity,
    estimated_unit_price, estimated_total_price, budget_amount, estimated_margin,
    needs_manual_review, quantity_check, stock_check, price_check
) VALUES
    (
        '66666666-6666-6666-6666-666666666601',
        '55555555-5555-5555-5555-555555555501',
        '44444444-4444-4444-4444-444444444401',
        '11111111-1111-1111-1111-111111111104',
        'suggested', 0.95,
        '4 МП, PoE, уличное исполнение — модель DS-2CD2043G2-I соответствует спецификации',
        '["Категория совпадает", "Бренд совпадает", "Достаточный остаток", "Цена указана"]'::jsonb,
        '["Проверить точную модель", "Проверить SKU", "Проверить гарантию"]'::jsonb,
        '["Требуется проверка модели", "Нужно проверить сертификаты", "Уточнить срок поставки"]'::jsonb,
        50, 120, 28500, 1425000, 4200000, 2775000,
        TRUE, 'ok', 'ok', 'ok'
    ),
    (
        '66666666-6666-6666-6666-666666666602',
        '55555555-5555-5555-5555-555555555502',
        '44444444-4444-4444-4444-444444444402',
        '11111111-1111-1111-1111-111111111104',
        'suggested', 0.65,
        'Mikrotik — альтернатива по категории сетевого оборудования, не Cisco',
        '["Категория совпадает", "Цена указана"]'::jsonb,
        '["Бренд не совпадает с ТЗ", "Проверить PoE+"]'::jsonb,
        '["Бренд не совпадает", "Требуется согласование заказчика"]'::jsonb,
        12, 45, 38990, 467880, 8900000, 8432120,
        TRUE, 'ok', 'ok', 'ok'
    ),
    (
        '66666666-6666-6666-6666-666666666603',
        '55555555-5555-5555-5555-555555555503',
        '44444444-4444-4444-4444-444444444403',
        '11111111-1111-1111-1111-111111111104',
        'suggested', 0.88,
        'HP ProLiant DL380 Gen10 — прямое соответствие спецификации серверов',
        '["Категория совпадает", "Бренд совпадает", "Модель совпадает"]'::jsonb,
        '["Проверить комплектацию PSU"]'::jsonb,
        '["Остаток ограничен — 8 шт"]'::jsonb,
        2, 8, 980000, 1960000, 2100000, 140000,
        TRUE, 'ok', 'ok', 'ok'
    )
ON CONFLICT (id) DO NOTHING;
