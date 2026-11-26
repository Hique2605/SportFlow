package com.example.SportFlow.Dto.Avaliacao;

import com.example.SportFlow.Entity.Avaliacao;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AvaliacaoResponse(
        UUID id,
        UUID agendamentoId,
        Short nota,
        String comentario,
        OffsetDateTime criadoEm
) {
    public static AvaliacaoResponse from(Avaliacao a) {
        return new AvaliacaoResponse(
                a.getId(), a.getAgendamento().getId(), a.getNota(), a.getComentario(), a.getCriadoEm()
        );
    }
}
