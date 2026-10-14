package com.example.SportFlow.Entity;

import com.example.SportFlow.Enum.PagamentoStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "pagamento")
@Getter
@Setter
@NoArgsConstructor
public class PagamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false, unique = true)
    private AgendamentoEntity agendamento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 30)
    private String forma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PagamentoStatus status = PagamentoStatus.PENDENTE;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "pago_em")
    private OffsetDateTime pagoEm;

    @PrePersist
    private void aoCriar() {
        criadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
