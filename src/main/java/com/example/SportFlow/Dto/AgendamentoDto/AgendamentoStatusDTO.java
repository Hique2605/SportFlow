package com.example.SportFlow.Dto.AgendamentoDto;

import com.example.SportFlow.Enum.StatusAgendamento;
import jakarta.validation.constraints.NotNull;

public record AgendamentoStatusDTO(@NotNull(message = "O status é obrigatório") StatusAgendamento status) {}
