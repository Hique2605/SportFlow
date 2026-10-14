package com.example.SportFlow.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AvaliacaoRequest(
        @NotNull UUID agendamentoId,
        @NotNull @Min(1) @Max(5) Short nota,
        @Size(max = 2000) String comentario
) {
}
