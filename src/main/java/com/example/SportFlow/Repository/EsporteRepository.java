package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.Esporte;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EsporteRepository extends JpaRepository<Esporte, UUID> {}
