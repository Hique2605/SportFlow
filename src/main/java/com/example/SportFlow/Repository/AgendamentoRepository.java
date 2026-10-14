package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.AgendamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgendamentoRepository extends JpaRepository<AgendamentoEntity, UUID> {
}
