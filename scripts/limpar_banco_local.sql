-- =====================================================================
-- Limpa o banco LOCAL: apaga todos os dados e deixa só o administrador.
--
-- NÃO é uma migration (não roda sozinho e não afeta o banco dos colegas).
-- Rode uma vez, conectado ao banco "sportflow":
--   pgAdmin: botão direito no banco sportflow > Query Tool > abrir este arquivo > Executar (F5)
--   psql:    psql -U postgres -d sportflow -f scripts/limpar_banco_local.sql
--
-- Tudo roda numa transação: se der erro no meio, nada é apagado.
-- A tabela flyway_schema_history (histórico das migrations) não é tocada.
-- =====================================================================
BEGIN;

-- 1) Filhos primeiro, por causa das chaves estrangeiras
DELETE FROM avaliacao;
DELETE FROM pagamento;
DELETE FROM agendamento;
DELETE FROM horario;

-- estabelecimento.funcionario_id aponta para funcionario (e vice-versa): solta antes
UPDATE estabelecimento SET funcionario_id = NULL;
DELETE FROM funcionario;
DELETE FROM quadra;
DELETE FROM estabelecimento;
DELETE FROM esporte;

-- 2) Todos os usuários, menos o administrador
DELETE FROM usuario WHERE id <> '00000000-0000-0000-0000-000000000001';

-- 3) Garante o login admin / admin (mesmo que a migration V1.3 ainda não tenha rodado)
UPDATE usuario
SET nome = 'Administrador',
    email = 'admin',
    senha = '$2a$10$M6DI5uG3mSa1YA2IuYXoduTM5cEH21ltLxN.5n1OnMfxuVPEB5JFK',
    perfil = 'ADMIN',
    ativo = TRUE,
    atualizado_em = CURRENT_TIMESTAMP
WHERE id = '00000000-0000-0000-0000-000000000001';

COMMIT;

-- Conferência: deve mostrar 1 usuário e zero no resto
SELECT 'usuario' AS tabela, COUNT(*) FROM usuario
UNION ALL SELECT 'estabelecimento', COUNT(*) FROM estabelecimento
UNION ALL SELECT 'quadra', COUNT(*) FROM quadra
UNION ALL SELECT 'horario', COUNT(*) FROM horario
UNION ALL SELECT 'esporte', COUNT(*) FROM esporte
UNION ALL SELECT 'agendamento', COUNT(*) FROM agendamento
UNION ALL SELECT 'pagamento', COUNT(*) FROM pagamento
UNION ALL SELECT 'avaliacao', COUNT(*) FROM avaliacao
UNION ALL SELECT 'funcionario', COUNT(*) FROM funcionario;
