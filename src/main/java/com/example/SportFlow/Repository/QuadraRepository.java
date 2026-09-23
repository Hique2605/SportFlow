package com.example.SportFlow.Repository;

import com.example.SportFlow.Entity.Quadra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface QuadraRepository extends JpaRepository<Quadra, UUID> {}
