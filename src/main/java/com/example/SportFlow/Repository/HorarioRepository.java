package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface HorarioRepository extends JpaRepository<Horario, UUID> {
    List<Horario> findByQuadraId(UUID quadraId);
}
