package com.example.SportFlow.Dto.IA;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MensagemAssistenteRequest(
        @Schema(example = "Quero jogar beach tennis sábado à tarde, até R$ 100")
        @NotBlank(message = "Escreva uma mensagem")
        @Size(max = 500, message = "A mensagem deve ter no máximo 500 caracteres")
        String mensagem
) {}
