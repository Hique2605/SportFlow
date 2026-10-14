package com.example.SportFlow.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record HorarioRequest(
        @NotNull UUID quadraId,
        @NotNull UUID esporteId,
        @NotNull @Min(0) @Max(6) Short diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim,
        Boolean ativo
) {
}
