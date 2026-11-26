package com.example.SportFlow.Dto.Funcionario;

import com.example.SportFlow.Entity.Funcionario;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuncionarioResponse(
        UUID id,
        UUID usuarioId,
        UUID estabelecimentoId,
        String cargo,
        LocalDate dataAdmissao,
        Boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
    public static FuncionarioResponse from(Funcionario f) {
        return new FuncionarioResponse(
                f.getId(), f.getUsuarioId(), f.getEstabelecimentoId(), f.getCargo(),
                f.getDataAdmissao(), f.getAtivo(), f.getCriadoEm(), f.getAtualizadoEm()
        );
    }
}
