package com.example.SportFlow.Dto.Estabelecimento;

import com.example.SportFlow.Entity.Estabelecimento;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EstabelecimentoResponse(
        UUID id,
        UUID funcionarioId,
        String nome,
        String email,
        String descricao,
        String telefone,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String cep,
        Boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
    public static EstabelecimentoResponse from(Estabelecimento e) {
        return new EstabelecimentoResponse(
                e.getId(), e.getFuncionarioId(), e.getNome(), e.getEmail(), e.getDescricao(),
                e.getTelefone(), e.getLogradouro(), e.getNumero(), e.getBairro(), e.getCidade(),
                e.getEstado(), e.getCep(), e.getAtivo(), e.getCriadoEm(), e.getAtualizadoEm()
        );
    }
}
