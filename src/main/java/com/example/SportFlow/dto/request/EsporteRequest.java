package com.example.SportFlow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EsporteRequest(
        @NotBlank @Size(max = 100) String nome,
        @Size(max = 2000) String descricao,
        Boolean ativo
) {
}
