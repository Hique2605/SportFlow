-- =====================================================================
-- V1.3 - Login do administrador simplificado
--   Usuário: admin
--   Senha:   admin   (hash BCrypt abaixo)
--
-- Observação: "admin" não é um e-mail válido nem tem 8 caracteres, por isso
-- a troca é feita direto no banco (as validações da API continuam valendo
-- para os demais usuários). Para usar em produção, troque essa senha.
-- =====================================================================
UPDATE usuario
SET nome          = 'Administrador',
    email         = 'admin',
    senha         = '$2a$10$M6DI5uG3mSa1YA2IuYXoduTM5cEH21ltLxN.5n1OnMfxuVPEB5JFK',
    perfil        = 'ADMIN',
    ativo         = TRUE,
    atualizado_em = CURRENT_TIMESTAMP
WHERE id = '00000000-0000-0000-0000-000000000001'
   OR email = 'admin@sportflow.com';
