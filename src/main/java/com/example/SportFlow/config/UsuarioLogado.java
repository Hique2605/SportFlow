package com.example.SportFlow.config;

import com.example.SportFlow.Enum.Perfil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.SportFlow.Entity.Funcionario;
import com.example.SportFlow.Repository.FuncionarioRepository;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Atalho para os services descobrirem QUEM está fazendo a requisição
 * (dados vindos do token JWT) e aplicarem as regras de acesso:
 *
 *   - "dono do recurso ou ADMIN"            -> verificarDonoOuAdmin
 *   - "gestor do estabelecimento ou ADMIN"  -> verificarGestor
 *
 * Gestor = usuário com vínculo de funcionário ATIVO no estabelecimento.
 * Não é um perfil novo: o mesmo CLIENTE pode jogar num lugar e gerenciar outro.
 */
@Component
public class UsuarioLogado {

    private final FuncionarioRepository funcionarioRepository;

    public UsuarioLogado(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

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

    // ------------------------------------------------------------ gestor

    /** Ids dos estabelecimentos em que o usuário logado é funcionário ativo. */
    public Set<UUID> estabelecimentosGerenciados() {
        return funcionarioRepository.findByUsuarioIdAndAtivoTrue(id()).stream()
                .map(Funcionario::getEstabelecimentoId)
                .collect(Collectors.toSet());
    }

    /** ADMIN pode tudo; os demais só se forem funcionários ativos do estabelecimento. */
    public boolean podeGerenciar(UUID estabelecimentoId) {
        return isAdmin()
                || (estabelecimentoId != null
                    && funcionarioRepository.existsByUsuarioIdAndEstabelecimentoIdAndAtivoTrue(id(), estabelecimentoId));
    }

    /** Lança 403 se o usuário logado não gerencia o estabelecimento (nem é ADMIN). */
    public void verificarGestor(UUID estabelecimentoId) {
        if (!podeGerenciar(estabelecimentoId)) {
            throw new AccessDeniedException("Você não gerencia este estabelecimento");
        }
    }

    /** Lança 403 se não for o dono do recurso, nem gestor do estabelecimento, nem ADMIN. */
    public void verificarDonoOuGestor(UUID donoId, UUID estabelecimentoId) {
        if (!id().equals(donoId) && !podeGerenciar(estabelecimentoId)) {
            throw new AccessDeniedException("Você não tem permissão para acessar este recurso");
        }
    }
}
