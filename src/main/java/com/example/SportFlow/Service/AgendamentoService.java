package com.example.SportFlow.Service;

import com.example.SportFlow.Dto.AgendamentoDto.AgendamentoRequestDTO;
import com.example.SportFlow.Dto.AgendamentoDto.AgendamentoResponseDTO;
import com.example.SportFlow.Entity.Agendamento;
import com.example.SportFlow.Entity.Horario;
import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Enum.StatusAgendamento;
import com.example.SportFlow.Repository.AgendamentoRepository;
import com.example.SportFlow.Repository.HorarioRepository;
import com.example.SportFlow.Repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HorarioRepository horarioRepository;

    @Transactional
    public AgendamentoResponseDTO criar(AgendamentoRequestDTO dto) {
        validarHorarios(dto);

        if (agendamentoRepository.existeConflito(
                dto.horarioId(), dto.dataAgendamento(),
                dto.horaInicio(), dto.horaFim(), null,
                StatusAgendamento.CANCELADO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe um agendamento nesse horário");
        }

        Agendamento entity = Agendamento.builder()
                .usuario(buscarUsuario(dto.usuarioId()))
                .horario(buscarHorario(dto.horarioId()))
                .dataAgendamento(dto.dataAgendamento())
                .horaInicio(dto.horaInicio())
                .horaFim(dto.horaFim())
                .valorTotal(dto.valorTotal())
                .observacao(dto.observacao())
                .build();

        return AgendamentoResponseDTO.fromEntity(agendamentoRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listar() {
        return agendamentoRepository.findAll().stream()
                .map(AgendamentoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponseDTO buscarPorId(UUID id) {
        return AgendamentoResponseDTO.fromEntity(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarPorUsuario(UUID usuarioId) {
        return agendamentoRepository.findByUsuarioId(usuarioId).stream()
                .map(AgendamentoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public AgendamentoResponseDTO atualizar(UUID id, AgendamentoRequestDTO dto) {
        validarHorarios(dto);
        Agendamento entity = buscarEntidade(id);

        if (agendamentoRepository.existeConflito(
                dto.horarioId(), dto.dataAgendamento(),
                dto.horaInicio(), dto.horaFim(), id,
                StatusAgendamento.CANCELADO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe um agendamento nesse horário");
        }

        entity.setUsuario(buscarUsuario(dto.usuarioId()));
        entity.setHorario(buscarHorario(dto.horarioId()));
        entity.setDataAgendamento(dto.dataAgendamento());
        entity.setHoraInicio(dto.horaInicio());
        entity.setHoraFim(dto.horaFim());
        entity.setValorTotal(dto.valorTotal());
        entity.setObservacao(dto.observacao());
        entity.setAtualizadoEm(OffsetDateTime.now());

        return AgendamentoResponseDTO.fromEntity(agendamentoRepository.save(entity));
    }

    @Transactional
    public AgendamentoResponseDTO atualizarStatus(UUID id, StatusAgendamento novoStatus) {
        Agendamento entity = buscarEntidade(id);
        entity.setStatus(novoStatus);
        entity.setAtualizadoEm(OffsetDateTime.now());
        return AgendamentoResponseDTO.fromEntity(agendamentoRepository.save(entity));
    }

    @Transactional
    public void cancelar(UUID id) {
        atualizarStatus(id, StatusAgendamento.CANCELADO);
    }

    @Transactional
    public void deletar(UUID id) {
        if (!agendamentoRepository.existsById(id)) {
            throw new EntityNotFoundException("Agendamento não encontrado: " + id);
        }
        agendamentoRepository.deleteById(id);
    }

    // ---------- métodos auxiliares ----------

    private Agendamento buscarEntidade(UUID id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado: " + id));
    }

    private void validarHorarios(AgendamentoRequestDTO dto) {
        if (!dto.horaFim().isAfter(dto.horaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A hora de fim deve ser posterior à hora de início");
        }
    }

    private Usuario buscarUsuario(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + id));
    }

    private Horario buscarHorario(UUID id) {
        return horarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Horário não encontrado: " + id));
    }
}