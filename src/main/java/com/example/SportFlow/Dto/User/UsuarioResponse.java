package com.example.SportFlow.Dto.User;

import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Enum.Perfil;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Saída de usuário: nunca expõe a senha. */
public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        Perfil perfil,
        boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(), u.getNome(), u.getEmail(), u.getTelefone(),
                u.getPerfil(), u.isAtivo(), u.getCriadoEm(), u.getAtualizadoEm()
        );
    }
}
