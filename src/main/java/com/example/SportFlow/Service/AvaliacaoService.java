package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.AvaliacaoEntity;
import com.example.SportFlow.Repository.AgendamentoRepository;
import com.example.SportFlow.Repository.AvaliacaoRepository;
import com.example.SportFlow.dto.request.AvaliacaoRequest;
import com.example.SportFlow.dto.response.AvaliacaoResponse;
import com.example.SportFlow.exception.ConflitoException;
import com.example.SportFlow.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository repository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listar() {
        return repository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public AvaliacaoResponse buscar(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    @Transactional
    public AvaliacaoResponse criar(AvaliacaoRequest request) {
        if (repository.existsByAgendamento_Id(request.agendamentoId())) {
            throw new ConflitoException("O agendamento já possui uma avaliação");
        }
        AvaliacaoEntity avaliacao = new AvaliacaoEntity();
        preencher(avaliacao, request);
        return paraResponse(repository.saveAndFlush(avaliacao));
    }

    @Transactional
    public AvaliacaoResponse atualizar(UUID id, AvaliacaoRequest request) {
        AvaliacaoEntity avaliacao = buscarEntidade(id);
        if (repository.existsByAgendamento_IdAndIdNot(request.agendamentoId(), id)) {
            throw new ConflitoException("O agendamento já possui uma avaliação");
        }
        preencher(avaliacao, request);
        return paraResponse(repository.saveAndFlush(avaliacao));
    }

    @Transactional
    public void excluir(UUID id) {
        repository.delete(buscarEntidade(id));
        repository.flush();
    }

    private AvaliacaoEntity buscarEntidade(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação não encontrada: " + id));
    }

    private void preencher(AvaliacaoEntity destino, AvaliacaoRequest origem) {
        destino.setAgendamento(agendamentoRepository.findById(origem.agendamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Agendamento não encontrado: " + origem.agendamentoId())));
        destino.setNota(origem.nota());
        destino.setComentario(origem.comentario() == null || origem.comentario().isBlank()
                ? null : origem.comentario().trim());
    }

    private AvaliacaoResponse paraResponse(AvaliacaoEntity avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.getId(),
                avaliacao.getAgendamento().getId(),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getCriadoEm(),
                avaliacao.getAtualizadoEm()
        );
    }
}
