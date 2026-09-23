package com.example.SportFlow.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "avaliacao")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvaliacaoEntity {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "agendamento_id",
            nullable = false,
            unique = true
    )
    private AgendamentoEntity agendamento;

    @Column(nullable = false)
    private Short nota;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;
}

