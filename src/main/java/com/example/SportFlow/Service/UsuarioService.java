package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;

    @Transactional
    public Usuario criar(Usuario usuario) {
        validarDados(usuario);
        validarSenha(usuario.getSenha());

        String email = normalizarEmail(usuario.getEmail());
        verificarEmailDisponivel(email);

        usuario.setNome(usuario.getNome().trim());
        usuario.setEmail(email);

        try {
            return repository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "E-mail já cadastrado", exception
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario buscar(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"
                )
        );
    }

    @Transactional
    public Usuario atualizar(UUID id, Usuario dados) {
        Usuario usuario = buscar(id);
        validarDados(dados);

        String email = normalizarEmail(dados.getEmail());

        if (!usuario.getEmail().equalsIgnoreCase(email)) {
            verificarEmailDisponivel(email);
        }

        usuario.setNome(dados.getNome().trim());
        usuario.setEmail(email);
        usuario.setTelefone(dados.getTelefone());
        usuario.setAtivo(dados.isAtivo());

        // Se a senha não vier no corpo da requisição, mantém a atual.
        if (dados.getSenha() != null) {
            validarSenha(dados.getSenha());
            usuario.setSenha(dados.getSenha());
        }

        try {
            return repository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "E-mail já cadastrado", exception
            );
        }
    }

    @Transactional
    public void excluir(UUID id) {
        Usuario usuario = buscar(id);

        try {
            repository.delete(usuario);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Usuário vinculado a outro registro não pode ser excluído",
                    exception
            );
        }
    }

    private void validarDados(Usuario usuario) {
        if (usuario == null
                || usuario.getNome() == null
                || usuario.getNome().isBlank()
                || usuario.getNome().trim().length() > 150
                || usuario.getEmail() == null
                || usuario.getEmail().isBlank()
                || usuario.getEmail().trim().length() > 254
                || !usuario.getEmail().contains("@")
                || (usuario.getTelefone() != null
                && usuario.getTelefone().length() > 30)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Dados do usuário inválidos"
            );
        }
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.length() < 8) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A senha deve ter pelo menos 8 caracteres"
            );
        }
    }

    private void verificarEmailDisponivel(String email) {
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "E-mail já cadastrado"
            );
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}