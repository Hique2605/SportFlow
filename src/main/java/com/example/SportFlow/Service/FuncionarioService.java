package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Funcionario;
import com.example.SportFlow.Repository.EstabelecimentoRepository;
import com.example.SportFlow.Repository.FuncionarioRepository;
import com.example.SportFlow.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;

    @Transactional
    public Funcionario criar(Funcionario dados) {
        validarDados(dados);
        validarReferencias(dados);

        if (repository.existsByUsuarioId(dados.getUsuarioId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este usuário já está cadastrado como funcionário"
            );
        }

        Funcionario funcionario = new Funcionario();
        preencher(funcionario, dados);

        return salvar(funcionario);
    }

    @Transactional(readOnly = true)
    public List<Funcionario> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Funcionario buscar(UUID id) {
        return encontrar(id);
    }

    @Transactional
    public Funcionario atualizar(UUID id, Funcionario dados) {
        Funcionario funcionario = encontrar(id);
        validarDados(dados);
        validarReferencias(dados);

        boolean mudouUsuario =
                !funcionario.getUsuarioId().equals(dados.getUsuarioId());

        if (mudouUsuario
                && repository.existsByUsuarioId(dados.getUsuarioId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este usuário já está cadastrado como funcionário"
            );
        }

        boolean mudouEstabelecimento =
                !funcionario.getEstabelecimentoId()
                        .equals(dados.getEstabelecimentoId());

        if (mudouEstabelecimento) {
            repository.removerComoResponsavel(id);
        }

        preencher(funcionario, dados);
        return salvar(funcionario);
    }

    @Transactional
    public void excluir(UUID id) {
        Funcionario funcionario = encontrar(id);

        // Remove o vínculo opcional de responsável antes de excluir.
        repository.removerComoResponsavel(id);

        try {
            repository.delete(funcionario);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O funcionário possui registros relacionados",
                    exception
            );
        }
    }

    private Funcionario encontrar(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário não encontrado"
                )
        );
    }

    private Funcionario salvar(Funcionario funcionario) {
        try {
            return repository.saveAndFlush(funcionario);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Os dados violam uma restrição do banco",
                    exception
            );
        }
    }

    private void validarDados(Funcionario dados) {
        if (dados == null
                || dados.getUsuarioId() == null
                || dados.getEstabelecimentoId() == null
                || dados.getCargo() == null
                || dados.getCargo().isBlank()
                || dados.getCargo().trim().length() > 100
                || dados.getDataAdmissao() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dados do funcionário inválidos"
            );
        }
    }

    private void validarReferencias(Funcionario dados) {
        if (!usuarioRepository.existsById(dados.getUsuarioId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usuário informado não existe"
            );
        }

        if (!estabelecimentoRepository.existsById(
                dados.getEstabelecimentoId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estabelecimento informado não existe"
            );
        }
    }

    private void preencher(Funcionario destino, Funcionario origem) {
        destino.setUsuarioId(origem.getUsuarioId());
        destino.setEstabelecimentoId(origem.getEstabelecimentoId());
        destino.setCargo(origem.getCargo().trim());
        destino.setDataAdmissao(origem.getDataAdmissao());

        if (origem.getAtivo() != null) {
            destino.setAtivo(origem.getAtivo());
        }
    }
}