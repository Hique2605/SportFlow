package com.example.SportFlow.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "agendamento")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendamentoEntity {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

//    @ManyToOne(fetch = FetchType.LAZY)REVER                          Descomentar quando criar a entidade UsuarioEntity
//    @JoinColumn(name = "usuario_id", nullable = false)
    //private UsuarioEntity usuario;

//    @ManyToOne(fetch = FetchType.LAZY)REVER                          Descomentar quando criar a entidade HorarioEntity
//    @JoinColumn(name = "horario_id", nullable = false)
//    private HorarioEntity horario;

    @Column(name = "data_agendamento", nullable = false)
    private LocalDate dataAgendamento;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDENTE";

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    @Builder.Default
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();
}
