package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Estabelecimento;
import com.example.SportFlow.Repository.EstabelecimentoRepository;
import com.example.SportFlow.dto.request.EstabelecimentoRequest;
import com.example.SportFlow.dto.response.EstabelecimentoResponse;
import com.example.SportFlow.exception.ConflitoException;
import com.example.SportFlow.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EstabelecimentoService {

    private final EstabelecimentoRepository repository;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<EstabelecimentoResponse> listar() {
        return repository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public EstabelecimentoResponse buscar(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    @Transactional
    public EstabelecimentoResponse criar(EstabelecimentoRequest request) {
        String email = normalizarEmail(request.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("E-mail do estabelecimento já cadastrado");
        }
        Estabelecimento estabelecimento = new Estabelecimento();
        preencher(estabelecimento, request, email);
        return paraResponse(repository.saveAndFlush(estabelecimento));
    }

    @Transactional
    public EstabelecimentoResponse atualizar(UUID id, EstabelecimentoRequest request) {
        Estabelecimento estabelecimento = buscarEntidade(id);
        String email = normalizarEmail(request.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflitoException("E-mail do estabelecimento já cadastrado");
        }
        preencher(estabelecimento, request, email);
        return paraResponse(repository.saveAndFlush(estabelecimento));
    }

    @Transactional
    public void excluir(UUID id) {
        repository.delete(buscarEntidade(id));
        repository.flush();
    }

    @Transactional(readOnly = true)
    public Estabelecimento buscarEntidade(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estabelecimento não encontrado: " + id));
    }

    private void preencher(Estabelecimento destino, EstabelecimentoRequest origem, String email) {
        destino.setGestor(usuarioService.buscarEntidade(origem.gestorId()));
        destino.setNome(origem.nome().trim());
        destino.setDescricao(normalizarOpcional(origem.descricao()));
        destino.setTelefone(normalizarOpcional(origem.telefone()));
        destino.setEmail(email);
        destino.setLogradouro(normalizarOpcional(origem.logradouro()));
        destino.setNumero(normalizarOpcional(origem.numero()));
        destino.setBairro(normalizarOpcional(origem.bairro()));
        destino.setCidade(normalizarOpcional(origem.cidade()));
        destino.setEstado(origem.estado() == null ? null : origem.estado().toUpperCase(Locale.ROOT));
        destino.setCep(normalizarOpcional(origem.cep()));
        if (origem.ativo() != null) {
            destino.setAtivo(origem.ativo());
        }
    }

    private EstabelecimentoResponse paraResponse(Estabelecimento estabelecimento) {
        return new EstabelecimentoResponse(
                estabelecimento.getId(),
                estabelecimento.getGestor() == null ? null : estabelecimento.getGestor().getId(),
                estabelecimento.getNome(),
                estabelecimento.getDescricao(),
                estabelecimento.getTelefone(),
                estabelecimento.getEmail(),
                estabelecimento.getLogradouro(),
                estabelecimento.getNumero(),
                estabelecimento.getBairro(),
                estabelecimento.getCidade(),
                estabelecimento.getEstado(),
                estabelecimento.getCep(),
                estabelecimento.isAtivo(),
                estabelecimento.getCriadoEm(),
                estabelecimento.getAtualizadoEm()
        );
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
