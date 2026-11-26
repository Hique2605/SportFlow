package com.example.SportFlow.Dto.Horario;

import com.example.SportFlow.Entity.Horario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record HorarioRequest(
        @Schema(description = "Id da quadra (pegue em GET /api/quadras)")
        @NotNull(message = "A quadra é obrigatória")
        UUID quadraId,

        @Schema(description = "Id do esporte (pegue em GET /api/esportes)")
        @NotNull(message = "O esporte é obrigatório")
        UUID esporteId,

        /** 0 = domingo, 1 = segunda ... 6 = sábado */
        @Schema(description = "0 = domingo, 1 = segunda ... 6 = sábado", example = "1")
        @NotNull(message = "O dia da semana é obrigatório")
        @Min(value = 0, message = "O dia da semana deve estar entre 0 (domingo) e 6 (sábado)")
        @Max(value = 6, message = "O dia da semana deve estar entre 0 (domingo) e 6 (sábado)")
        Short diaSemana,

        @Schema(type = "string", format = "time", example = "08:00:00")
        @NotNull(message = "A hora de início é obrigatória")
        LocalTime horaInicio,

        @Schema(type = "string", format = "time", example = "10:00:00")
        @NotNull(message = "A hora de fim é obrigatória")
        LocalTime horaFim,

        @Schema(example = "true")
        Boolean ativo
) {
    public Horario toEntity() {
        Horario h = new Horario();
        h.setQuadraId(quadraId);
        h.setEsporteId(esporteId);
        h.setDiaSemana(diaSemana);
        h.setHoraInicio(horaInicio);
        h.setHoraFim(horaFim);
        h.setAtivo(ativo);
        return h;
    }
}
