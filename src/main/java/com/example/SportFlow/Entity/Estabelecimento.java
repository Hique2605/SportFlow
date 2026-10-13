package com.example.SportFlow.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "estabelecimento")
@Getter
@NoArgsConstructor
public class Estabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    @Setter
    @Column(name = "funcionario_id")
    private UUID funcionarioId;

    @Setter
    @Column(nullable = false, length = 150)
    private String nome;

    @Setter
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Setter
    @Column(length = 30)
    private String telefone;

    @Setter
    @Column(length = 200)
    private String logradouro;

    @Setter
    @Column(length = 20)
    private String numero;

    @Setter
    @Column(length = 100)
    private String bairro;

    @Setter
    @Column(length = 100)
    private String cidade;

    @Setter
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 2, columnDefinition = "CHAR(2)")
    private String estado;

    @Setter
    @Column(length = 9)
    private String cep;

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