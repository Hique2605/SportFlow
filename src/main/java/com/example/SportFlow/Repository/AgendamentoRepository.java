package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.Agendamento;
import com.example.SportFlow.Enum.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID> {

    List<Agendamento> findByUsuarioId(UUID usuarioId);

    List<Agendamento> findByHorarioId(UUID horarioId);

    List<Agendamento> findByStatus(StatusAgendamento status);

    /** Reservas em certos status com data até o dia informado (usado pelo encerramento automático). */
    List<Agendamento> findByStatusInAndDataAgendamentoLessThanEqual(Collection<StatusAgendamento> status, LocalDate data);

    /** Agendamentos das quadras dos estabelecimentos informados (visão do gestor). */
    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.horario.quadraId IN (
                SELECT q.id FROM Quadra q WHERE q.estabelecimentoId IN :estabelecimentoIds
            )
            """)
    List<Agendamento> findByEstabelecimentos(@Param("estabelecimentoIds") Collection<UUID> estabelecimentoIds);

    /**
     * Existe outro agendamento (não cancelado) na MESMA QUADRA, na mesma data,
     * com intervalo de horas sobreposto? Antes a checagem era só pelo mesmo
     * horário, o que permitia reservar a quadra duas vezes por horários diferentes.
     */
    @Query("""
            SELECT COUNT(a) > 0 FROM Agendamento a
            WHERE a.horario.quadraId = :quadraId
              AND a.dataAgendamento = :data
              AND a.status <> :statusCancelado
              AND a.horaInicio < :horaFim
              AND a.horaFim > :horaInicio
              AND (:ignorarId IS NULL OR a.id <> :ignorarId)
            """)
    boolean existeConflito(@Param("quadraId") UUID quadraId,
                           @Param("data") LocalDate data,
                           @Param("horaInicio") LocalTime horaInicio,
                           @Param("horaFim") LocalTime horaFim,
                           @Param("ignorarId") UUID ignorarId,
                           @Param("statusCancelado") StatusAgendamento statusCancelado);
}
