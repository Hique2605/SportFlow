-- =====================================================================
-- V1.2 - Perfis de acesso e ajuste do status de pagamento
-- Script idempotente: pode rodar em bancos que já receberam ajustes
-- parecidos manualmente (IF EXISTS / IF NOT EXISTS em tudo).
-- =====================================================================

-- 1) Perfil de acesso do usuário (usado na autorização com JWT)
ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS perfil VARCHAR(20) NOT NULL DEFAULT 'CLIENTE';

ALTER TABLE usuario DROP CONSTRAINT IF EXISTS ck_usuario_perfil;
ALTER TABLE usuario
    ADD CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMIN', 'CLIENTE'));

-- 2) Status do pagamento volta a ser VARCHAR (igual ao status do agendamento).
--    O tipo ENUM nativo do PostgreSQL (criado na V1.1) não aceita o texto enviado
--    pelo Hibernate com @Enumerated(EnumType.STRING) e quebrava o INSERT.
--    A restrição de valores continua garantida pela CHECK constraint.
ALTER TABLE pagamento ALTER COLUMN status DROP DEFAULT;
ALTER TABLE pagamento ALTER COLUMN status TYPE VARCHAR(30) USING status::text;
ALTER TABLE pagamento ALTER COLUMN status SET DEFAULT 'PENDENTE';

ALTER TABLE pagamento DROP CONSTRAINT IF EXISTS ck_pagamento_status;
ALTER TABLE pagamento
    ADD CONSTRAINT ck_pagamento_status
        CHECK (status IN ('PENDENTE', 'PAGO', 'CANCELADO', 'ESTORNADO'));

DROP TYPE IF EXISTS pagamento_status;

-- 3) Status do agendamento também ganha CHECK constraint
ALTER TABLE agendamento DROP CONSTRAINT IF EXISTS ck_agendamento_status;
ALTER TABLE agendamento
    ADD CONSTRAINT ck_agendamento_status
        CHECK (status IN ('PENDENTE', 'CONFIRMADO', 'CANCELADO', 'CONCLUIDO'));

-- 4) Usuário administrador inicial
--    E-mail: admin@sportflow.com | Senha: Admin@123 (hash BCrypt abaixo)
INSERT INTO usuario (id, nome, email, senha, perfil)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Administrador SportFlow',
        'admin@sportflow.com',
        '$2a$10$rCduBNeQJTqDyHred5dLSuvIjniYRUVf1qr.kK592yazKSm6.m8Ei',
        'ADMIN')
ON CONFLICT (email) DO UPDATE SET perfil = 'ADMIN';
