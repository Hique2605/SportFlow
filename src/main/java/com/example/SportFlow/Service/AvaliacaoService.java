package com.example.SportFlow.Service;

import com.example.SportFlow.Dto.Avaliacao.AvaliacaoRequest;
import com.example.SportFlow.Dto.Avaliacao.AvaliacaoResponse;
import com.example.SportFlow.Dto.Avaliacao.AvaliacaoUpdateRequest;
import com.example.SportFlow.Entity.Agendamento;
import com.example.SportFlow.Entity.Avaliacao;
import com.example.SportFlow.Enum.StatusAgendamento;
import com.example.SportFlow.Repository.AgendamentoRepository;
import com.example.SportFlow.Repository.AvaliacaoRepository;
import com.example.SportFlow.config.UsuarioLogado;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioLogado usuarioLogado;

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listar() {
        return avaliacaoRepository.findAll().stream().map(AvaliacaoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AvaliacaoResponse buscar(UUID id) {
        return AvaliacaoResponse.from(encontrar(id));
    }

    /** Só o dono do agendamento (ou ADMIN) avalia, uma vez, e nunca um agendamento cancelado. */
    @Transactional
    public AvaliacaoResponse criar(AvaliacaoRequest dto) {
        Agendamento agendamento = agendamentoRepository.findById(dto.agendamentoId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado: " + dto.agendamentoId()));

        usuarioLogado.verificarDonoOuAdmin(agendamento.getUsuario().getId());

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new IllegalStateException("Não é possível avaliar um agendamento cancelado");
        }

        if (avaliacaoRepository.existsByAgendamentoId(dto.agendamentoId())) {
            throw new IllegalStateException("Este agendamento já foi avaliado");
        }

        Avaliacao avaliacao = Avaliacao.builder()
                .agendamento(agendamento)
                .nota(dto.nota())
                .comentario(dto.comentario())
                .build();

        Avaliacao salva = avaliacaoRepository.save(avaliacao);
        log.info("Avaliação {} (nota {}) registrada para o agendamento {}", salva.getId(), dto.nota(), agendamento.getId());
        return AvaliacaoResponse.from(salva);
    }

    @Transactional
    public AvaliacaoResponse atualizar(UUID id, AvaliacaoUpdateRequest dto) {
        Avaliacao avaliacao = encontrar(id);
        usuarioLogado.verificarDonoOuAdmin(avaliacao.getAgendamento().getUsuario().getId());

        avaliacao.setNota(dto.nota());
        avaliacao.setComentario(dto.comentario());

        return AvaliacaoResponse.from(avaliacaoRepository.save(avaliacao));
    }

    @Transactional
    public void excluir(UUID id) {
        Avaliacao avaliacao = encontrar(id);
        usuarioLogado.verificarDonoOuAdmin(avaliacao.getAgendamento().getUsuario().getId());
        avaliacaoRepository.delete(avaliacao);
    }

    private Avaliacao encontrar(UUID id) {
        return avaliacaoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Avaliação não encontrada: " + id));
    }
}
