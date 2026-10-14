package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Enum.PerfilUsuario;
import com.example.SportFlow.Repository.UsuarioRepository;
import com.example.SportFlow.dto.request.RegisterRequest;
import com.example.SportFlow.dto.request.UsuarioCreateRequest;
import com.example.SportFlow.dto.request.UsuarioUpdateRequest;
import com.example.SportFlow.dto.response.UsuarioResponse;
import com.example.SportFlow.exception.ConflitoException;
import com.example.SportFlow.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse criar(UsuarioCreateRequest request) {
        return criarInterno(
                request.nome(), request.email(), request.senha(), request.telefone(), PerfilUsuario.CLIENTE
        );
    }

    @Transactional
    public UsuarioResponse registrar(RegisterRequest request) {
        return criarInterno(
                request.nome(), request.email(), request.senha(), request.telefone(), PerfilUsuario.CLIENTE
        );
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse atualizar(UUID id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarEntidade(id);
        String email = normalizarEmail(request.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflitoException("E-mail já cadastrado");
        }

        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setTelefone(normalizarOpcional(request.telefone()));
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
        }
        if (request.ativo() != null) {
            usuario.setAtivo(request.ativo());
        }
        return paraResponse(repository.saveAndFlush(usuario));
    }

    @Transactional
    public void excluir(UUID id) {
        repository.delete(buscarEntidade(id));
        repository.flush();
    }

    @Transactional(readOnly = true)
    public Usuario buscarEntidade(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
    }

    private UsuarioResponse criarInterno(
            String nome,
            String emailInformado,
            String senha,
            String telefone,
            PerfilUsuario perfil
    ) {
        String email = normalizarEmail(emailInformado);
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("E-mail já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome.trim());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setTelefone(normalizarOpcional(telefone));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return paraResponse(repository.saveAndFlush(usuario));
    }

    public UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getPerfil(),
                usuario.isAtivo(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm()
        );
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
