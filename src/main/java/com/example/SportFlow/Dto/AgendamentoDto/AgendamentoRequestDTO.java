package com.example.SportFlow.Dto.AgendamentoDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * usuarioId é opcional: se não vier, o agendamento é feito para o usuário do token.
 * Só ADMIN pode agendar em nome de outro usuário.
 * O valorTotal não é mais enviado: é calculado pelo servidor (valor/hora da quadra x duração).
 */
public record AgendamentoRequestDTO(
        @Schema(description = "Opcional: sem ele, o agendamento é do dono do token", nullable = true)
        UUID usuarioId,

        @NotNull(message = "O horário é obrigatório")
        UUID horarioId,

        @Schema(description = "Precisa cair no diaSemana do horário", example = "2026-11-30")
        @NotNull(message = "A data é obrigatória")
        @FutureOrPresent(message = "A data do agendamento não pode estar no passado")
        LocalDate dataAgendamento,

        @Schema(type = "string", format = "time", example = "08:00:00")
        @NotNull(message = "A hora de início é obrigatória")
        LocalTime horaInicio,

        @Schema(type = "string", format = "time", example = "09:00:00")
        @NotNull(message = "A hora de fim é obrigatória")
        LocalTime horaFim,

        @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres")
        String observacao
) {}
