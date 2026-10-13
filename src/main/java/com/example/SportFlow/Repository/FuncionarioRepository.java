package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {

    boolean existsByUsuarioId(UUID usuarioId);

    @Modifying
    @Query(value = """
            UPDATE estabelecimento
            SET funcionario_id = NULL,
                atualizado_em = CURRENT_TIMESTAMP
            WHERE funcionario_id = :funcionarioId
            """, nativeQuery = true)
    int removerComoResponsavel(
            @Param("funcionarioId") UUID funcionarioId
    );
}