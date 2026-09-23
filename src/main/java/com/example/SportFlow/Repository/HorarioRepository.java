package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface HorarioRepository extends JpaRepository<Horario, UUID> {}
