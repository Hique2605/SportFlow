package com.example.SportFlow.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EstabelecimentoResponse(
        UUID id,
        UUID gestorId,
        String nome,
        String descricao,
        String telefone,
        String email,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String cep,
        boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
}
