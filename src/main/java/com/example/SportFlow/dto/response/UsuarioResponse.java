package com.example.SportFlow.dto.response;

import com.example.SportFlow.Enum.PerfilUsuario;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        PerfilUsuario perfil,
        boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
}
