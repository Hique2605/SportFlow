package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Estabelecimento;
import com.example.SportFlow.Repository.EstabelecimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EstabelecimentoService {

    private final EstabelecimentoRepository repository;

    @Transactional
    public Estabelecimento criar(Estabelecimento dados) {
        validarDados(dados);

        if (dados.getFuncionarioId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Crie o estabelecimento e o funcionário antes de indicar o responsável"
            );
        }

        String email = normalizarEmail(dados.getEmail());
        verificarEmailDisponivel(email);

        Estabelecimento estabelecimento = new Estabelecimento();
        preencher(estabelecimento, dados, email);

        return salvar(estabelecimento);
    }

    @Transactional(readOnly = true)
    public List<Estabelecimento> listar() {
        return repository.findAll(Sort.by("nome"));
    }

    @Transactional(readOnly = true)
    public Estabelecimento buscar(UUID id) {
        return encontrar(id);
    }

    @Transactional
    public Estabelecimento atualizar(UUID id, Estabelecimento dados) {
        Estabelecimento estabelecimento = encontrar(id);
        validarDados(dados);

        String email = normalizarEmail(dados.getEmail());

        if (!estabelecimento.getEmail().equalsIgnoreCase(email)) {
            verificarEmailDisponivel(email);
        }

        if (dados.getFuncionarioId() != null) {
            boolean pertenceAoEstabelecimento =
                    repository.contarFuncionarioDoEstabelecimento(
                            dados.getFuncionarioId(), id
                    ) > 0;

            if (!pertenceAoEstabelecimento) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "O funcionário indicado não pertence a este estabelecimento"
                );
            }

            estabelecimento.setFuncionarioId(dados.getFuncionarioId());
        }

        preencher(estabelecimento, dados, email);
        return salvar(estabelecimento);
    }

    @Transactional
    public void excluir(UUID id) {
        Estabelecimento estabelecimento = encontrar(id);

        if (repository.contarFuncionarios(id) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O estabelecimento possui funcionários e não pode ser excluído"
            );
        }

        try {
            repository.delete(estabelecimento);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O estabelecimento possui registros relacionados",
                    exception
            );
        }
    }

    private Estabelecimento encontrar(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Estabelecimento não encontrado"
                )
        );
    }

    private Estabelecimento salvar(Estabelecimento estabelecimento) {
        try {
            return repository.saveAndFlush(estabelecimento);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Os dados violam uma restrição do banco",
                    exception
            );
        }
    }

    private void verificarEmailDisponivel(String email) {
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "E-mail já cadastrado"
            );
        }
    }

    private void validarDados(Estabelecimento dados) {
        if (dados == null
                || dados.getNome() == null
                || dados.getNome().isBlank()
                || dados.getNome().trim().length() > 150
                || dados.getEmail() == null
                || dados.getEmail().isBlank()
                || dados.getEmail().trim().length() > 254
                || !dados.getEmail().contains("@")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nome ou e-mail inválido"
            );
        }

        validarTamanho(dados.getTelefone(), 30, "telefone");
        validarTamanho(dados.getLogradouro(), 200, "logradouro");
        validarTamanho(dados.getNumero(), 20, "número");
        validarTamanho(dados.getBairro(), 100, "bairro");
        validarTamanho(dados.getCidade(), 100, "cidade");
        validarTamanho(dados.getCep(), 9, "CEP");

        if (dados.getEstado() != null
                && dados.getEstado().trim().length() != 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O estado deve ter duas letras"
            );
        }
    }

    private void validarTamanho(String valor, int limite, String campo) {
        if (valor != null && valor.length() > limite) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O campo " + campo + " excede o limite de " + limite
            );
        }
    }

    private void preencher(
            Estabelecimento destino,
            Estabelecimento origem,
            String email
    ) {
        destino.setNome(origem.getNome().trim());
        destino.setEmail(email);
        destino.setDescricao(origem.getDescricao());
        destino.setTelefone(origem.getTelefone());
        destino.setLogradouro(origem.getLogradouro());
        destino.setNumero(origem.getNumero());
        destino.setBairro(origem.getBairro());
        destino.setCidade(origem.getCidade());
        destino.setCep(origem.getCep());

        if (origem.getEstado() != null) {
            destino.setEstado(
                    origem.getEstado().trim().toUpperCase(Locale.ROOT)
            );
        } else {
            destino.setEstado(null);
        }

        if (origem.getAtivo() != null) {
            destino.setAtivo(origem.getAtivo());
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}