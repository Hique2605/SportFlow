package com.example.SportFlow.Enum;

/**
 * Ciclo de vida de uma reserva:
 *
 *   PENDENTE ──► CONFIRMADO ──► CONCLUIDO
 *       │             │
 *       └──► CANCELADO ◄┘
 *
 * CANCELADO e CONCLUIDO são finais: para jogar de novo, faz-se uma nova reserva.
 */
public enum StatusAgendamento {
    PENDENTE,
    CONFIRMADO,
    CANCELADO,
    CONCLUIDO;

    /** A reserva pode passar deste status para o novo? */
    public boolean podeIrPara(StatusAgendamento novo) {
        return switch (this) {
            case PENDENTE -> novo == CONFIRMADO || novo == CANCELADO;
            case CONFIRMADO -> novo == CONCLUIDO || novo == CANCELADO;
            case CANCELADO, CONCLUIDO -> false;
        };
    }

    /** Status em que a reserva já terminou e não muda mais. */
    public boolean isFinal() {
        return this == CANCELADO || this == CONCLUIDO;
    }
}