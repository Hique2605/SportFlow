package com.example.SportFlow.Dto.AgendamentoDto;

import com.example.SportFlow.Enum.StatusAgendamento;
import jakarta.validation.constraints.NotNull;

public record AgendamentoStatusDTO(@NotNull StatusAgendamento status) {}