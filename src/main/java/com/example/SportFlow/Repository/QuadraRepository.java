package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.Quadra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface QuadraRepository extends JpaRepository<Quadra, UUID> {

    /**
     * Trava a linha da quadra até o fim da transação (SELECT ... FOR UPDATE).
     * Duas reservas simultâneas para a MESMA quadra passam a ser feitas uma de cada vez:
     * a segunda espera a primeira gravar e, quando verifica o conflito, já enxerga a reserva dela.
     * Reservas de quadras diferentes não se bloqueiam.
     */
    @Query(value = "SELECT id FROM quadra WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<UUID> travarParaReserva(@Param("id") UUID id);
}
