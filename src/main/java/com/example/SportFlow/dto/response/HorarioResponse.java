package com.example.SportFlow.dto.response;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HorarioResponse(
        UUID id,
        UUID quadraId,
        UUID esporteId,
        Short diaSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
}
