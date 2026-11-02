package com.example.SportFlow.Dto.Pagamento;

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
) {}