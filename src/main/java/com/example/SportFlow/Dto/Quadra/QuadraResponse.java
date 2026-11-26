package com.example.SportFlow.Dto.Quadra;

import com.example.SportFlow.Entity.Quadra;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record QuadraResponse(
        UUID id,
        UUID estabelecimentoId,
        String nome,
        String descricao,
        BigDecimal valorHora,
        Boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
    public static QuadraResponse from(Quadra q) {
        return new QuadraResponse(
                q.getId(), q.getEstabelecimentoId(), q.getNome(), q.getDescricao(),
                q.getValorHora(), q.getAtivo(), q.getCriadoEm(), q.getAtualizadoEm()
        );
    }
}
