package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.Agendamento;
import com.example.SportFlow.Enum.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID> {


    List<Agendamento> findByUsuarioId(UUID usuarioId);

    List<Agendamento> findByHorarioId(UUID horarioId);

    List<Agendamento> findByStatus(StatusAgendamento status);

    @Query("""
    SELECT COUNT(a) > 0 FROM Agendamento a
    WHERE a.horario.id = :horarioId
      AND a.dataAgendamento = :data
      AND a.status <> :statusCancelado
      AND a.horaInicio < :horaFim
      AND a.horaFim > :horaInicio
      AND (:ignorarId IS NULL OR a.id <> :ignorarId)
""")
    boolean existeConflito(@Param("horarioId") UUID horarioId,
                           @Param("data") LocalDate data,
                           @Param("horaInicio") LocalTime horaInicio,
                           @Param("horaFim") LocalTime horaFim,
                           @Param("ignorarId") UUID ignorarId,
                           @Param("statusCancelado") StatusAgendamento statusCancelado);
}


