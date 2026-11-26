package com.example.SportFlow.Dto.Estabelecimento;

import com.example.SportFlow.Entity.Estabelecimento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EstabelecimentoRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
        String email,

        String descricao,

        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
        String telefone,

        @Size(max = 200, message = "O logradouro deve ter no máximo 200 caracteres")
        String logradouro,

        @Size(max = 20, message = "O número deve ter no máximo 20 caracteres")
        String numero,

        @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres")
        String bairro,

        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
        String cidade,

        @Pattern(regexp = "^[A-Za-z]{2}$", message = "O estado deve ter 2 letras (ex.: SC)")
        String estado,

        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido (use 88800000 ou 88800-000)")
        String cep,

        /** Só pode ser informado na atualização (o funcionário precisa existir antes). */
        UUID funcionarioId,

        Boolean ativo
) {
    public Estabelecimento toEntity() {
        Estabelecimento e = new Estabelecimento();
        e.setNome(nome);
        e.setEmail(email);
        e.setDescricao(descricao);
        e.setTelefone(telefone);
        e.setLogradouro(logradouro);
        e.setNumero(numero);
        e.setBairro(bairro);
        e.setCidade(cidade);
        e.setEstado(estado);
        e.setCep(cep);
        e.setFuncionarioId(funcionarioId);
        e.setAtivo(ativo);
        return e;
    }
}
