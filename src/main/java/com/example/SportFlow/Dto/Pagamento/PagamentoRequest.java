package com.example.SportFlow.Dto.Pagamento;

import java.math.BigDecimal;
import java.util.UUID;

public record PagamentoRequest(
        UUID agendamentoId,
        BigDecimal valor,
        String forma
) {}