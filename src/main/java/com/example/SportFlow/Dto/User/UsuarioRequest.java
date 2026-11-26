package com.example.SportFlow.Dto.User;

import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Enum.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Criação de usuário pelo ADMIN (permite escolher o perfil). */
public record UsuarioRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senha,

        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
        String telefone,

        Boolean ativo,

        /** Opcional. Padrão: CLIENTE */
        Perfil perfil
) {
    public Usuario toEntity() {
        Usuario u = new Usuario(nome, email, senha, telefone);
        u.setAtivo(ativo == null || ativo);
        u.setPerfil(perfil == null ? Perfil.CLIENTE : perfil);
        return u;
    }
}
