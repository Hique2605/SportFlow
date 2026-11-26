package com.example.SportFlow.config;

import com.example.SportFlow.Enum.Perfil;
import lombok.Builder;

import java.util.UUID;

/**
 * Dados extraídos do token JWT. Fica guardado como "principal" no
 * SecurityContext durante a requisição.
 */
@Builder
public record JWTUserData(UUID userId, String email, Perfil perfil) {
}
