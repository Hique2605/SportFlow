package com.example.SportFlow.Dto.AgendamentoDto;

import jakarta.validation.constraints.*;
import org.antlr.v4.runtime.misc.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AgendamentoRequestDTO(
        @NotNull UUID usuarioId,
        @NotNull UUID horarioId,
        @NotNull @FutureOrPresent LocalDate dataAgendamento,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim,
        @NotNull @DecimalMin("0.0") BigDecimal valorTotal,
        String observacao
) {}