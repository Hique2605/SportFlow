package com.example.SportFlow.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "funcionario")
@Getter
@NoArgsConstructor
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    @Setter
    @Column(name = "usuario_id", nullable = false, unique = true)
    private UUID usuarioId;

    @Setter
    @Column(name = "estabelecimento_id", nullable = false)
    private UUID estabelecimentoId;

    @Setter
    @Column(nullable = false, length = 100)
    private String cargo;

    @Setter
    @Column(name = "data_admissao", nullable = false)
    private LocalDate dataAdmissao;

    @Setter
    @Column(nullable = false)
    private Boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    private void aoCriar() {
        if (ativo == null) {
            ativo = true;
        }

        criadoEm = OffsetDateTime.now(ZoneOffset.UTC);
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    private void aoAtualizar() {
        atualizadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }
}