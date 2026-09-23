package com.example.SportFlow.Entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.OffsetDateTime;

@Entity
@Table(name = "esporte")
public class Esporte {
    @Id
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        criadoEm = OffsetDateTime.now();
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    void onUpdate() { atualizadoEm = OffsetDateTime.now(); }

    public UUID getId() { return id; }

    public String getNome() { return nome; }
    public void setNome(String value) { this.nome = value; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String value) { this.descricao = value; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }

    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
