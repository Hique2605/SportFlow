package com.example.SportFlow.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AvaliacaoResponse(
        UUID id,
        UUID agendamentoId,
        Short nota,
        String comentario,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
}
