package com.example.SportFlow.Dto.Esporte;

import com.example.SportFlow.Entity.Esporte;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EsporteResponse(
        UUID id,
        String nome,
        String descricao,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
    public static EsporteResponse from(Esporte e) {
        return new EsporteResponse(e.getId(), e.getNome(), e.getDescricao(), e.getCriadoEm(), e.getAtualizadoEm());
    }
}
