package com.example.SportFlow.Dto.IA;

import com.example.SportFlow.Service.DisponibilidadeService.OpcaoLivre;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Uma opção que a IA recomendou, já com os dados REAIS do banco.
 * O front usa horarioId + dataAgendamento para fazer o POST /agendamentos normal.
 */
public record SugestaoReservaResponse(
        UUID horarioId,
        LocalDate dataAgendamento,
        LocalTime horaInicio,
        LocalTime horaFim,
        UUID estabelecimentoId,
        String estabelecimentoNome,
        String quadraNome,
        String esporteNome,
        BigDecimal valor,
        Double notaMedia
) {
    public static SugestaoReservaResponse from(OpcaoLivre o) {
        return new SugestaoReservaResponse(o.horarioId(), o.data(), o.horaInicio(), o.horaFim(),
                o.estabelecimentoId(), o.estabelecimentoNome(), o.quadraNome(), o.esporteNome(),
                o.valor(), o.notaMedia());
    }
}
