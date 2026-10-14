ALTER TABLE usuario
    ADD COLUMN perfil VARCHAR(20) NOT NULL DEFAULT 'CLIENTE';

ALTER TABLE usuario
    ADD CONSTRAINT ck_usuario_perfil
        CHECK (perfil IN ('CLIENTE', 'FUNCIONARIO', 'ADMIN'));

ALTER TABLE estabelecimento
    DROP COLUMN IF EXISTS funcionario_id;

ALTER TABLE estabelecimento
    ADD COLUMN gestor_id UUID;

ALTER TABLE estabelecimento
    ADD CONSTRAINT fk_estabelecimento_gestor
        FOREIGN KEY (gestor_id) REFERENCES usuario (id);

ALTER TABLE funcionario
    ADD CONSTRAINT fk_funcionario_estabelecimento
        FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento (id);

ALTER TABLE quadra
    ADD COLUMN tipo_piso VARCHAR(60);

ALTER TABLE quadra
    ADD COLUMN coberta BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE esporte
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE avaliacao
    ADD COLUMN atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE agendamento
    ADD CONSTRAINT uq_agendamento_horario_data
        UNIQUE (horario_id, data_agendamento);

CREATE INDEX idx_estabelecimento_gestor_id ON estabelecimento (gestor_id);
