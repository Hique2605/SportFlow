package com.example.SportFlow.Entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "quadra")
public class Quadra {
    @Id
    private UUID id;

    @Column(name = "estabelecimento_id", nullable = false)
    private UUID estabelecimentoId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "valor_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorHora;

    @Column(nullable = false)
    private Boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (ativo == null) {
            ativo = true;
        }

        criadoEm = OffsetDateTime.now();
        atualizadoEm = criadoEm;
    }
    @PreUpdate
    void onUpdate() { atualizadoEm = OffsetDateTime.now(); }

    public UUID getId() { return id; }

    public UUID getEstabelecimentoId() { return estabelecimentoId; }
    public void setEstabelecimentoId(UUID value) { this.estabelecimentoId = value; }

    public String getNome() { return nome; }
    public void setNome(String value) { this.nome = value; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String value) { this.descricao = value; }

    public BigDecimal getValorHora() { return valorHora; }
    public void setValorHora(BigDecimal value) { this.valorHora = value; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean value) { this.ativo = value; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }

    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
