package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Horario;
import com.example.SportFlow.Repository.HorarioRepository;
import com.example.SportFlow.dto.request.HorarioRequest;
import com.example.SportFlow.dto.response.HorarioResponse;
import com.example.SportFlow.exception.ConflitoException;
import com.example.SportFlow.exception.RecursoNaoEncontradoException;
import com.example.SportFlow.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HorarioService {

    private final HorarioRepository repository;
    private final QuadraService quadraService;
    private final EsporteService esporteService;

    @Transactional(readOnly = true)
    public List<HorarioResponse> listar() {
        return repository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public HorarioResponse buscar(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    @Transactional
    public HorarioResponse criar(HorarioRequest request) {
        validarIntervalo(request);
        if (repository.existsByQuadra_IdAndDiaSemanaAndHoraInicioLessThanAndHoraFimGreaterThan(
                request.quadraId(), request.diaSemana(), request.horaFim(), request.horaInicio())) {
            throw new ConflitoException("Já existe um horário sobreposto para esta quadra e dia da semana");
        }
        Horario horario = new Horario();
        preencher(horario, request);
        return paraResponse(repository.saveAndFlush(horario));
    }

    @Transactional
    public HorarioResponse atualizar(UUID id, HorarioRequest request) {
        Horario horario = buscarEntidade(id);
        validarIntervalo(request);
        if (repository.existsByQuadra_IdAndDiaSemanaAndIdNotAndHoraInicioLessThanAndHoraFimGreaterThan(
                request.quadraId(), request.diaSemana(), id, request.horaFim(), request.horaInicio())) {
            throw new ConflitoException("Já existe um horário sobreposto para esta quadra e dia da semana");
        }
        preencher(horario, request);
        return paraResponse(repository.saveAndFlush(horario));
    }

    @Transactional
    public void excluir(UUID id) {
        repository.delete(buscarEntidade(id));
        repository.flush();
    }

    @Transactional(readOnly = true)
    public Horario buscarEntidade(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Horário não encontrado: " + id));
    }

    private void preencher(Horario destino, HorarioRequest origem) {
        destino.setQuadra(quadraService.buscarEntidade(origem.quadraId()));
        destino.setEsporte(esporteService.buscarEntidade(origem.esporteId()));
        destino.setDiaSemana(origem.diaSemana());
        destino.setHoraInicio(origem.horaInicio());
        destino.setHoraFim(origem.horaFim());
        if (origem.ativo() != null) {
            destino.setAtivo(origem.ativo());
        }
    }

    private void validarIntervalo(HorarioRequest request) {
        if (!request.horaFim().isAfter(request.horaInicio())) {
            throw new RegraNegocioException("A hora final deve ser posterior à hora inicial");
        }
    }

    private HorarioResponse paraResponse(Horario horario) {
        return new HorarioResponse(
                horario.getId(),
                horario.getQuadra().getId(),
                horario.getEsporte().getId(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFim(),
                horario.isAtivo(),
                horario.getCriadoEm(),
                horario.getAtualizadoEm()
        );
    }
}
