package com.example.SportFlow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record QuadraRequest(
        @NotNull UUID estabelecimentoId,
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 2000) String descricao,
        @Size(max = 60) String tipoPiso,
        Boolean coberta,
        @NotNull @PositiveOrZero BigDecimal valorHora,
        Boolean ativo
) {
}
