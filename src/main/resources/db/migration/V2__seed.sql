-- tenants
INSERT INTO tenants (id, slug, name, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111', 'acme', 'Acme Corporation', NOW(), NOW()),
('22222222-2222-2222-2222-222222222222', 'globex', 'Globex Corporation', NOW(), NOW());

-- users: password = "password" for all (BCrypt $2a$10$6SCachyQGuqkVJHPgFyb5e.wUYI/BDZ1I5o0.SkQKkIcfh0jqsUvK)
INSERT INTO users (id, tenant_id, username, password_hash, role, email, created_at, updated_at) VALUES
('a0000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'admin',    '$2a$10$6SCachyQGuqkVJHPgFyb5e.wUYI/BDZ1I5o0.SkQKkIcfh0jqsUvK', 'ADMIN',    'admin@acme.com', NOW(), NOW()),
('a0000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'manager',  '$2a$10$6SCachyQGuqkVJHPgFyb5e.wUYI/BDZ1I5o0.SkQKkIcfh0jqsUvK', 'MANAGER',  'manager@acme.com', NOW(), NOW()),
('a0000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'employee', '$2a$10$6SCachyQGuqkVJHPgFyb5e.wUYI/BDZ1I5o0.SkQKkIcfh0jqsUvK', 'EMPLOYEE', 'employee@acme.com', NOW(), NOW()),
('b0000000-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'admin',    '$2a$10$6SCachyQGuqkVJHPgFyb5e.wUYI/BDZ1I5o0.SkQKkIcfh0jqsUvK', 'ADMIN',    'admin@globex.com', NOW(), NOW()),
('b0000000-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', 'manager',  '$2a$10$6SCachyQGuqkVJHPgFyb5e.wUYI/BDZ1I5o0.SkQKkIcfh0jqsUvK', 'MANAGER',  'manager@globex.com', NOW(), NOW()),
('b0000000-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222222', 'employee', '$2a$10$6SCachyQGuqkVJHPgFyb5e.wUYI/BDZ1I5o0.SkQKkIcfh0jqsUvK', 'EMPLOYEE', 'employee@globex.com', NOW(), NOW());