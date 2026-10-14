package com.example.SportFlow.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record QuadraResponse(
        UUID id,
        UUID estabelecimentoId,
        String nome,
        String descricao,
        String tipoPiso,
        boolean coberta,
        BigDecimal valorHora,
        boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
}
