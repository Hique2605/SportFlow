package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    Optional<Pagamento> findByAgendamentoId(UUID agendamentoId);

    boolean existsByAgendamentoId(UUID agendamentoId);
}