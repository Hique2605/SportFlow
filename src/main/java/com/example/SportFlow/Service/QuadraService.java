package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Quadra;
import com.example.SportFlow.Repository.QuadraRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class QuadraService {

    private final QuadraRepository repository;

    public QuadraService(QuadraRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Quadra> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Quadra buscar(UUID id) {
        return encontrar(id);
    }

    @Transactional
    public Quadra criar(Quadra dados) {
        validar(dados);

        Quadra quadra = new Quadra();
        preencher(quadra, dados);

        try {
            return repository.saveAndFlush(quadra);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estabelecimento inexistente ou dados inválidos",
                    exception
            );
        }
    }

    @Transactional
    public Quadra atualizar(UUID id, Quadra dados) {
        validar(dados);

        Quadra quadra = encontrar(id);
        preencher(quadra, dados);

        try {
            return repository.saveAndFlush(quadra);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estabelecimento inexistente ou dados inválidos",
                    exception
            );
        }
    }

    @Transactional
    public void excluir(UUID id) {
        Quadra quadra = encontrar(id);

        try {
            repository.delete(quadra);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A quadra não pode ser excluída porque possui registros relacionados",
                    exception
            );
        }
    }

    private Quadra encontrar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Quadra inexistente: " + id
                ));
    }

    private void validar(Quadra quadra) {
        if (quadra == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os dados da quadra são obrigatórios"
            );
        }

        if (quadra.getEstabelecimentoId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O estabelecimento é obrigatório"
            );
        }

        if (quadra.getNome() == null || quadra.getNome().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O nome da quadra é obrigatório"
            );
        }

        if (quadra.getValorHora() == null ||
                quadra.getValorHora().signum() < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O valor por hora deve ser maior ou igual a zero"
            );
        }
    }

    private void preencher(Quadra destino, Quadra origem) {
        destino.setEstabelecimentoId(origem.getEstabelecimentoId());
        destino.setNome(origem.getNome().trim());
        destino.setDescricao(origem.getDescricao());
        destino.setValorHora(origem.getValorHora());

        if (origem.getAtivo() != null) {
            destino.setAtivo(origem.getAtivo());
        }
    }
}