package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.AvaliacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AvaliacaoRepository extends JpaRepository<AvaliacaoEntity,UUID> {

}
