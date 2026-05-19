-- Creates the first admin account.
-- Password hash below = BCrypt of 'admin1234' (strength 12).
-- Change the password via PATCH /api/admin/users/{id}/role after first login,
-- or regenerate the hash with: htpasswd -bnBC 12 "" admin1234 | tr -d ':\n'
INSERT INTO users (full_name, email, password_hash, role)
VALUES ('Admin', 'admin@claims-mvp.local',
        '$2a$12$9K1tHpPDUM9j3zH5tqoY2OmEFuB.LnVDROmh3K8OxXmFD3X6GGSG2',
        'ADMIN') ON CONFLICT (email) DO NOTHING;