package com.example.SportFlow.Dto.Esporte;

import com.example.SportFlow.Entity.Esporte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EsporteRequest(
        @NotBlank(message = "O nome do esporte é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String descricao
) {
    public Esporte toEntity() {
        Esporte esporte = new Esporte();
        esporte.setNome(nome);
        esporte.setDescricao(descricao);
        return esporte;
    }
}
