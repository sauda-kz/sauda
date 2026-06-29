-- Demo distributor organization and users for dev/test auth (password: Sauda123!)
-- BCrypt hash generated with BCryptPasswordEncoder strength 10.

INSERT INTO organization (id, type, name, bin, vat_payer) VALUES
    ('11111111-1111-1111-1111-111111111104', 'distributor', 'Distributor Supply A', '123456789014', TRUE),
    ('11111111-1111-1111-1111-111111111105', 'distributor', 'Distributor Supply B', '123456789015', FALSE);

INSERT INTO app_user (id, organization_id, email, password_hash, is_active) VALUES
    (
        '22222222-2222-2222-2222-222222222204',
        '11111111-1111-1111-1111-111111111104',
        'distributor-a@shop.kz',
        '$2a$10$uJVxtlnkf4NMuC3q5cQNZedtxXXpvX3MbOeJiVe/Exy8tl8h0tMo.',
        TRUE
    ),
    (
        '22222222-2222-2222-2222-222222222205',
        '11111111-1111-1111-1111-111111111105',
        'distributor-b@shop.kz',
        '$2a$10$uJVxtlnkf4NMuC3q5cQNZedtxXXpvX3MbOeJiVe/Exy8tl8h0tMo.',
        TRUE
    );

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN app_role r ON r.code = 'distributor_manager'
WHERE u.email = 'distributor-a@shop.kz';

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN app_role r ON r.code = 'distributor_viewer'
WHERE u.email = 'distributor-b@shop.kz';
