package com.example.SportFlow.Dto.Funcionario;

import com.example.SportFlow.Entity.Funcionario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record FuncionarioRequest(
        @NotNull(message = "O usuário é obrigatório")
        UUID usuarioId,

        @NotNull(message = "O estabelecimento é obrigatório")
        UUID estabelecimentoId,

        @NotBlank(message = "O cargo é obrigatório")
        @Size(max = 100, message = "O cargo deve ter no máximo 100 caracteres")
        String cargo,

        @NotNull(message = "A data de admissão é obrigatória")
        @PastOrPresent(message = "A data de admissão não pode estar no futuro")
        LocalDate dataAdmissao,

        Boolean ativo
) {
    public Funcionario toEntity() {
        Funcionario f = new Funcionario();
        f.setUsuarioId(usuarioId);
        f.setEstabelecimentoId(estabelecimentoId);
        f.setCargo(cargo);
        f.setDataAdmissao(dataAdmissao);
        f.setAtivo(ativo);
        return f;
    }
}
