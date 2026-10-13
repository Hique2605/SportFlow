package com.example.SportFlow.Dto.AgendamentoDto;

import com.example.SportFlow.Entity.Agendamento;
import com.example.SportFlow.Enum.StatusAgendamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AgendamentoResponseDTO(
        UUID id,
        UUID usuarioId,
        UUID horarioId,
        LocalDate dataAgendamento,
        LocalTime horaInicio,
        LocalTime horaFim,
        BigDecimal valorTotal,
        StatusAgendamento status,
        String observacao,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
    public static AgendamentoResponseDTO fromEntity(Agendamento a) {
        return new AgendamentoResponseDTO(
                a.getId(),
                a.getUsuario().getId(),
                a.getHorario().getId(),
                a.getDataAgendamento(),
                a.getHoraInicio(),
                a.getHoraFim(),
                a.getValorTotal(),
                a.getStatus(),
                a.getObservacao(),
                a.getCriadoEm(),
                a.getAtualizadoEm()
        );
    }
}