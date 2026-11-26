package com.example.SportFlow.Dto.Horario;

import com.example.SportFlow.Entity.Horario;

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
        Boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
    public static HorarioResponse from(Horario h) {
        return new HorarioResponse(
                h.getId(), h.getQuadraId(), h.getEsporteId(), h.getDiaSemana(),
                h.getHoraInicio(), h.getHoraFim(), h.getAtivo(), h.getCriadoEm(), h.getAtualizadoEm()
        );
    }
}
