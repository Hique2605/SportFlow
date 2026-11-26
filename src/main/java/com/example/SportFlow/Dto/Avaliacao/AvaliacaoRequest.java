package com.example.SportFlow.Dto.Avaliacao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AvaliacaoRequest(
        @NotNull(message = "O agendamento é obrigatório")
        UUID agendamentoId,

        @NotNull(message = "A nota é obrigatória")
        @Min(value = 1, message = "A nota deve ser entre 1 e 5")
        @Max(value = 5, message = "A nota deve ser entre 1 e 5")
        Short nota,

        @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres")
        String comentario
) {}
