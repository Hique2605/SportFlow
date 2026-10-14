package com.example.SportFlow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EstabelecimentoRequest(
        @NotNull UUID gestorId,
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 2000) String descricao,
        @Size(max = 30) String telefone,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 200) String logradouro,
        @Size(max = 20) String numero,
        @Size(max = 100) String bairro,
        @Size(max = 100) String cidade,
        @Pattern(regexp = "(?i)^[a-z]{2}$", message = "estado deve conter duas letras") String estado,
        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido") String cep,
        Boolean ativo
) {
}
