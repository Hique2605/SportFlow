package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Quadra;
import com.example.SportFlow.Repository.QuadraRepository;
import com.example.SportFlow.dto.request.QuadraRequest;
import com.example.SportFlow.dto.response.QuadraResponse;
import com.example.SportFlow.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuadraService {

    private final QuadraRepository repository;
    private final EstabelecimentoService estabelecimentoService;

    @Transactional(readOnly = true)
    public List<QuadraResponse> listar() {
        return repository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public QuadraResponse buscar(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    @Transactional
    public QuadraResponse criar(QuadraRequest request) {
        Quadra quadra = new Quadra();
        preencher(quadra, request);
        return paraResponse(repository.saveAndFlush(quadra));
    }

    @Transactional
    public QuadraResponse atualizar(UUID id, QuadraRequest request) {
        Quadra quadra = buscarEntidade(id);
        preencher(quadra, request);
        return paraResponse(repository.saveAndFlush(quadra));
    }

    @Transactional
    public void excluir(UUID id) {
        repository.delete(buscarEntidade(id));
        repository.flush();
    }

    @Transactional(readOnly = true)
    public Quadra buscarEntidade(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra não encontrada: " + id));
    }

    private void preencher(Quadra destino, QuadraRequest origem) {
        destino.setEstabelecimento(estabelecimentoService.buscarEntidade(origem.estabelecimentoId()));
        destino.setNome(origem.nome().trim());
        destino.setDescricao(normalizarOpcional(origem.descricao()));
        destino.setTipoPiso(normalizarOpcional(origem.tipoPiso()));
        destino.setValorHora(origem.valorHora());
        if (origem.coberta() != null) {
            destino.setCoberta(origem.coberta());
        }
        if (origem.ativo() != null) {
            destino.setAtivo(origem.ativo());
        }
    }

    private QuadraResponse paraResponse(Quadra quadra) {
        return new QuadraResponse(
                quadra.getId(),
                quadra.getEstabelecimento().getId(),
                quadra.getNome(),
                quadra.getDescricao(),
                quadra.getTipoPiso(),
                quadra.isCoberta(),
                quadra.getValorHora(),
                quadra.isAtivo(),
                quadra.getCriadoEm(),
                quadra.getAtualizadoEm()
        );
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
