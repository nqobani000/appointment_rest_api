ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(100);

-- Seeded existing clients; all share the same BCrypt-hashed password.
INSERT INTO users (id, name, surname, email, phone, password_hash)
VALUES ('b1000000-0000-4000-8000-000000000001', 'Lerato', 'Mokoena', 'lerato.mokoena@example.com', '+27821110001',
        '$2a$10$cdSqkY4FcVgBwSgiHR2r4.8xCjPaL9xOhvsoBYNZxdM933oZKvV/2'),
       ('b1000000-0000-4000-8000-000000000002', 'Sipho', 'Dlamini', 'sipho.dlamini@example.com', '+27821110002',
        '$2a$10$cdSqkY4FcVgBwSgiHR2r4.8xCjPaL9xOhvsoBYNZxdM933oZKvV/2'),
       ('b1000000-0000-4000-8000-000000000003', 'Ayesha', 'Patel', 'ayesha.patel@example.com', '+27821110003',
        '$2a$10$cdSqkY4FcVgBwSgiHR2r4.8xCjPaL9xOhvsoBYNZxdM933oZKvV/2'),
       ('b1000000-0000-4000-8000-000000000004', 'Pieter', 'van Wyk', 'pieter.vanwyk@example.com', '+27821110004',
        '$2a$10$cdSqkY4FcVgBwSgiHR2r4.8xCjPaL9xOhvsoBYNZxdM933oZKvV/2')
ON CONFLICT (email) DO NOTHING;

INSERT INTO customers (id, name, surname, email, phone, customer_type, user_id)
SELECT gen_random_uuid(), u.name, u.surname, u.email, u.phone, 'CLIENT', u.id
FROM users u
WHERE u.id IN ('b1000000-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000002',
               'b1000000-0000-4000-8000-000000000003', 'b1000000-0000-4000-8000-000000000004')
ON CONFLICT (user_id) DO NOTHING;
