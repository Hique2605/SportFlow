package com.example.SportFlow.Entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "horario")
public class Horario {
    @Id
    private UUID id;

    @Column(name = "quadra_id", nullable = false)
    private UUID quadraId;

    @Column(name = "esporte_id", nullable = false)
    private UUID esporteId;

    @Column(name = "dia_semana", nullable = false)
    private Short diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

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

    public UUID getQuadraId() { return quadraId; }
    public void setQuadraId(UUID value) { this.quadraId = value; }

    public UUID getEsporteId() { return esporteId; }
    public void setEsporteId(UUID value) { this.esporteId = value; }

    public Short getDiaSemana() { return diaSemana; }
    public void setDiaSemana(Short value) { this.diaSemana = value; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime value) { this.horaInicio = value; }

    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime value) { this.horaFim = value; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean value) { this.ativo = value; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }

    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
