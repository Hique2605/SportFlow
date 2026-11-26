package com.example.SportFlow.config;

import com.example.SportFlow.Enum.Perfil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Atalho para os services descobrirem QUEM está fazendo a requisição
 * (dados vindos do token JWT) e aplicarem a regra "dono do recurso ou ADMIN".
 */
@Component
public class UsuarioLogado {

    public JWTUserData dados() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JWTUserData dados) {
            return dados;
        }
        throw new AccessDeniedException("Usuário não autenticado");
    }

    public UUID id() {
        return dados().userId();
    }

    public boolean isAdmin() {
        return dados().perfil() == Perfil.ADMIN;
    }

    /** Lança 403 se o usuário logado não for o dono do recurso nem ADMIN. */
    public void verificarDonoOuAdmin(UUID donoId) {
        if (!isAdmin() && !id().equals(donoId)) {
            throw new AccessDeniedException("Você não tem permissão para acessar este recurso");
        }
    }
}
