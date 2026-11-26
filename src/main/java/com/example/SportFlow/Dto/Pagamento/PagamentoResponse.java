package com.example.SportFlow.Dto.Pagamento;

import com.example.SportFlow.Entity.Pagamento;
import com.example.SportFlow.Enum.PagamentoStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PagamentoResponse(
        UUID id,
        UUID agendamentoId,
        BigDecimal valor,
        String forma,
        PagamentoStatus status,
        OffsetDateTime criadoEm,
        OffsetDateTime pagoEm
) {
    public static PagamentoResponse from(Pagamento p) {
        return new PagamentoResponse(
                p.getId(), p.getAgendamento().getId(), p.getValor(), p.getForma(),
                p.getStatus(), p.getCriadoEm(), p.getPagoEm()
        );
    }
}
