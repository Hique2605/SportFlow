package com.example.SportFlow.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EsporteResponse(
        UUID id,
        String nome,
        String descricao,
        boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
}
