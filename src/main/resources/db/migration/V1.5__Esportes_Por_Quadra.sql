-- =====================================================================
-- V1.5 - Quais esportes cada quadra aceita (relação muitos-para-muitos)
--
-- Ex.: "Areia 1" aceita Beach Tennis e Vôlei de Praia, mas não Futebol.
-- O horário continua tendo o seu esporte, e o backend só aceita um
-- esporte que esteja na lista da quadra.
-- =====================================================================
CREATE TABLE IF NOT EXISTS quadra_esporte (
    quadra_id  UUID NOT NULL,
    esporte_id UUID NOT NULL,
    CONSTRAINT pk_quadra_esporte PRIMARY KEY (quadra_id, esporte_id),
    CONSTRAINT fk_quadra_esporte_quadra
        FOREIGN KEY (quadra_id) REFERENCES quadra (id) ON DELETE CASCADE,
    CONSTRAINT fk_quadra_esporte_esporte
        FOREIGN KEY (esporte_id) REFERENCES esporte (id)
);

-- Para não quebrar nada: cada quadra passa a aceitar os esportes
-- que ela já tinha nos horários cadastrados.
INSERT INTO quadra_esporte (quadra_id, esporte_id)
SELECT DISTINCT quadra_id, esporte_id FROM horario
ON CONFLICT DO NOTHING;
