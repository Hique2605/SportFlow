package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EstabelecimentoRepository
        extends JpaRepository<Estabelecimento, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    @Query(value = """
            SELECT COUNT(*)
            FROM funcionario
            WHERE id = :funcionarioId
              AND estabelecimento_id = :estabelecimentoId
            """, nativeQuery = true)
    long contarFuncionarioDoEstabelecimento(
            @Param("funcionarioId") UUID funcionarioId,
            @Param("estabelecimentoId") UUID estabelecimentoId
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM funcionario
            WHERE estabelecimento_id = :estabelecimentoId
            """, nativeQuery = true)
    long contarFuncionarios(
            @Param("estabelecimentoId") UUID estabelecimentoId
    );
}