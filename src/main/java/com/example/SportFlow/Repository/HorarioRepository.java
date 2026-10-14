package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.Horario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.UUID;

public interface HorarioRepository extends JpaRepository<Horario, UUID> {
    boolean existsByQuadra_IdAndDiaSemanaAndHoraInicioLessThanAndHoraFimGreaterThan(
            UUID quadraId, Short diaSemana, LocalTime horaFim, LocalTime horaInicio);

    boolean existsByQuadra_IdAndDiaSemanaAndIdNotAndHoraInicioLessThanAndHoraFimGreaterThan(
            UUID quadraId, Short diaSemana, UUID id, LocalTime horaFim, LocalTime horaInicio);
}
