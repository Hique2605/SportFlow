package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Esporte;
import com.example.SportFlow.Repository.EsporteRepository;
import com.example.SportFlow.dto.request.EsporteRequest;
import com.example.SportFlow.dto.response.EsporteResponse;
import com.example.SportFlow.exception.ConflitoException;
import com.example.SportFlow.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EsporteService {

    private final EsporteRepository repository;

    @Transactional(readOnly = true)
    public List<EsporteResponse> listar() {
        return repository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public EsporteResponse buscar(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    @Transactional
    public EsporteResponse criar(EsporteRequest request) {
        String nome = request.nome().trim();
        if (repository.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Esporte já cadastrado");
        }
        Esporte esporte = new Esporte();
        preencher(esporte, request, nome);
        return paraResponse(repository.saveAndFlush(esporte));
    }

    @Transactional
    public EsporteResponse atualizar(UUID id, EsporteRequest request) {
        Esporte esporte = buscarEntidade(id);
        String nome = request.nome().trim();
        if (repository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new ConflitoException("Esporte já cadastrado");
        }
        preencher(esporte, request, nome);
        return paraResponse(repository.saveAndFlush(esporte));
    }

    @Transactional
    public void excluir(UUID id) {
        repository.delete(buscarEntidade(id));
        repository.flush();
    }

    @Transactional(readOnly = true)
    public Esporte buscarEntidade(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Esporte não encontrado: " + id));
    }

    private void preencher(Esporte destino, EsporteRequest origem, String nome) {
        destino.setNome(nome);
        destino.setDescricao(origem.descricao() == null || origem.descricao().isBlank()
                ? null : origem.descricao().trim());
        if (origem.ativo() != null) {
            destino.setAtivo(origem.ativo());
        }
    }

    private EsporteResponse paraResponse(Esporte esporte) {
        return new EsporteResponse(
                esporte.getId(),
                esporte.getNome(),
                esporte.getDescricao(),
                esporte.isAtivo(),
                esporte.getCriadoEm(),
                esporte.getAtualizadoEm()
        );
    }
}
