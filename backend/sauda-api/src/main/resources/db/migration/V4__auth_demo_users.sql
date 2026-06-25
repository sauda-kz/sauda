-- Demo organizations and users for dev/test auth (password: Sauda123!)
-- BCrypt hash generated with BCryptPasswordEncoder strength 10.

INSERT INTO organization (id, type, name, bin, vat_payer) VALUES
    ('11111111-1111-1111-1111-111111111101', 'platform', 'Sauda Platform', NULL, FALSE),
    ('11111111-1111-1111-1111-111111111102', 'buyer', 'Buyer Shop A', '123456789012', TRUE),
    ('11111111-1111-1111-1111-111111111103', 'buyer', 'Buyer Shop B', '123456789013', FALSE);

INSERT INTO app_user (id, organization_id, email, password_hash, is_active) VALUES
    (
        '22222222-2222-2222-2222-222222222201',
        '11111111-1111-1111-1111-111111111101',
        'admin@sauda.kz',
        '$2a$10$uJVxtlnkf4NMuC3q5cQNZedtxXXpvX3MbOeJiVe/Exy8tl8h0tMo.',
        TRUE
    ),
    (
        '22222222-2222-2222-2222-222222222202',
        '11111111-1111-1111-1111-111111111102',
        'buyer-a@shop.kz',
        '$2a$10$uJVxtlnkf4NMuC3q5cQNZedtxXXpvX3MbOeJiVe/Exy8tl8h0tMo.',
        TRUE
    ),
    (
        '22222222-2222-2222-2222-222222222203',
        '11111111-1111-1111-1111-111111111103',
        'buyer-b@shop.kz',
        '$2a$10$uJVxtlnkf4NMuC3q5cQNZedtxXXpvX3MbOeJiVe/Exy8tl8h0tMo.',
        TRUE
    );

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN app_role r ON r.code = 'platform_admin'
WHERE u.email = 'admin@sauda.kz';

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN app_role r ON r.code = 'buyer'
WHERE u.email IN ('buyer-a@shop.kz', 'buyer-b@shop.kz');
