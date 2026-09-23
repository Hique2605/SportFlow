package com.example.SportFlow.Entity;

import com.example.SportFlow.Enum.PagamentoStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagamento")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoEntity {

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

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 30)
    private String forma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PagamentoStatus status;

    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "pago_em")
    private OffsetDateTime pagoEm;
}

