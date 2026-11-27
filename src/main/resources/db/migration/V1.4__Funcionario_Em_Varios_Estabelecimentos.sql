-- =====================================================================
-- V1.4 - Funcionário (gestor) pode trabalhar em VÁRIOS estabelecimentos
--
-- Antes: funcionario.usuario_id era UNIQUE -> cada usuário só podia ser
-- funcionário de UM estabelecimento.
-- Agora: o mesmo usuário pode aparecer várias vezes, mas só uma vez por
-- estabelecimento (UNIQUE no par usuario_id + estabelecimento_id).
--
-- Todo funcionário ATIVO gerencia o estabelecimento ao qual está ligado
-- (regra no código: UsuarioLogado.verificarGestor).
-- =====================================================================

-- 1) Remove a restrição antiga (nome padrão gerado pelo PostgreSQL)
ALTER TABLE funcionario DROP CONSTRAINT IF EXISTS funcionario_usuario_id_key;

-- 2) Nova regra: um vínculo por usuário + estabelecimento
ALTER TABLE funcionario DROP CONSTRAINT IF EXISTS uk_funcionario_usuario_estabelecimento;
ALTER TABLE funcionario
    ADD CONSTRAINT uk_funcionario_usuario_estabelecimento UNIQUE (usuario_id, estabelecimento_id);

-- 3) Chave estrangeira que faltava (funcionario -> estabelecimento).
--    Vínculos que apontam para estabelecimento inexistente são inválidos e saem antes.
UPDATE estabelecimento SET funcionario_id = NULL
WHERE funcionario_id IN (
    SELECT id FROM funcionario
    WHERE estabelecimento_id NOT IN (SELECT id FROM estabelecimento)
);
DELETE FROM funcionario WHERE estabelecimento_id NOT IN (SELECT id FROM estabelecimento);

ALTER TABLE funcionario DROP CONSTRAINT IF EXISTS fk_funcionario_estabelecimento;
ALTER TABLE funcionario
    ADD CONSTRAINT fk_funcionario_estabelecimento
        FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento (id);
