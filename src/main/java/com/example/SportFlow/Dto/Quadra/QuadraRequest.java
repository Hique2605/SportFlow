package com.example.SportFlow.Dto.Quadra;

import com.example.SportFlow.Entity.Quadra;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record QuadraRequest(
        @NotNull(message = "O estabelecimento é obrigatório")
        UUID estabelecimentoId,

        @NotBlank(message = "O nome da quadra é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        String descricao,

        @NotNull(message = "O valor por hora é obrigatório")
        @DecimalMin(value = "0.0", message = "O valor por hora não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Valor inválido (máx. 8 dígitos e 2 casas decimais)")
        BigDecimal valorHora,

        Boolean ativo,

        @Schema(description = "Ids dos esportes que a quadra aceita (pegue em GET /api/esportes)")
        @NotEmpty(message = "Informe pelo menos um esporte que a quadra aceita")
        Set<UUID> esporteIds
) {
    public Quadra toEntity() {
        Quadra q = new Quadra();
        q.setEstabelecimentoId(estabelecimentoId);
        q.setNome(nome);
        q.setDescricao(descricao);
        q.setValorHora(valorHora);
        q.setAtivo(ativo);
        q.setEsporteIds(esporteIds);
        return q;
    }
}
