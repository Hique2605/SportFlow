package com.example.SportFlow.Service;

import com.example.SportFlow.Dto.AgendamentoDto.AgendamentoRequestDTO;
import com.example.SportFlow.Dto.AgendamentoDto.AgendamentoResponseDTO;
import com.example.SportFlow.Entity.Agendamento;
import com.example.SportFlow.Entity.Horario;
import com.example.SportFlow.Entity.Quadra;
import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Enum.PagamentoStatus;
import com.example.SportFlow.Enum.StatusAgendamento;
import com.example.SportFlow.Repository.AgendamentoRepository;
import com.example.SportFlow.Repository.HorarioRepository;
import com.example.SportFlow.Repository.PagamentoRepository;
import com.example.SportFlow.Repository.QuadraRepository;
import com.example.SportFlow.Repository.UsuarioRepository;
import com.example.SportFlow.config.UsuarioLogado;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.example.SportFlow.Repository.EstabelecimentoRepository;
import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HorarioRepository horarioRepository;
    private final QuadraRepository quadraRepository;
    private final PagamentoRepository pagamentoRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final UsuarioLogado usuarioLogado;
    private final Clock relogio;

    /** Status que o encerramento automático procura. */
    private static final List<StatusAgendamento> EM_ABERTO =
            List.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO);

    @Transactional
    public AgendamentoResponseDTO criar(AgendamentoRequestDTO dto) {
        UUID usuarioId = resolverUsuario(dto.usuarioId());
        Horario horario = buscarHorario(dto.horarioId());
        Quadra quadra = travarQuadra(horario.getQuadraId());

        validarRegras(dto, horario, quadra, null);

        Agendamento entity = Agendamento.builder()
                .usuario(buscarUsuario(usuarioId))
                .horario(horario)
                .dataAgendamento(dto.dataAgendamento())
                .horaInicio(dto.horaInicio())
                .horaFim(dto.horaFim())
                .valorTotal(calcularValor(quadra, dto))
                .observacao(dto.observacao())
                .build();

        Agendamento salvo = agendamentoRepository.save(entity);
        log.info("Agendamento {} criado para o usuário {} em {} das {} às {}",
                salvo.getId(), usuarioId, dto.dataAgendamento(), dto.horaInicio(), dto.horaFim());
        return AgendamentoResponseDTO.fromEntity(salvo);
    }

    /**
     * ADMIN vê todos.
     * Os demais veem os próprios + os dos estabelecimentos que gerenciam (gestor).
     */
    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listar() {
        if (usuarioLogado.isAdmin()) {
            return agendamentoRepository.findAll().stream().map(AgendamentoResponseDTO::fromEntity).toList();
        }

        // LinkedHashMap pelo id: junta as duas listas sem repetir
        Map<UUID, Agendamento> todos = new LinkedHashMap<>();
        agendamentoRepository.findByUsuarioId(usuarioLogado.id()).forEach(a -> todos.put(a.getId(), a));

        Set<UUID> gerenciados = usuarioLogado.estabelecimentosGerenciados();
        if (!gerenciados.isEmpty()) {
            agendamentoRepository.findByEstabelecimentos(gerenciados).forEach(a -> todos.put(a.getId(), a));
        }

        return todos.values().stream().map(AgendamentoResponseDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponseDTO buscarPorId(UUID id) {
        Agendamento agendamento = buscarEntidade(id);
        usuarioLogado.verificarDonoOuGestor(agendamento.getUsuario().getId(), estabelecimentoDe(agendamento));
        return AgendamentoResponseDTO.fromEntity(agendamento);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarPorUsuario(UUID usuarioId) {
        usuarioLogado.verificarDonoOuAdmin(usuarioId);
        return agendamentoRepository.findByUsuarioId(usuarioId).stream()
                .map(AgendamentoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public AgendamentoResponseDTO atualizar(UUID id, AgendamentoRequestDTO dto) {
        Agendamento entity = buscarEntidade(id);
        usuarioLogado.verificarDonoOuAdmin(entity.getUsuario().getId());

        if (entity.getStatus() != StatusAgendamento.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente agendamentos pendentes podem ser alterados");
        }

        // CLIENTE não pode transferir o agendamento para outra pessoa
        UUID usuarioId = dto.usuarioId() == null ? entity.getUsuario().getId() : resolverUsuario(dto.usuarioId());
        Horario horario = buscarHorario(dto.horarioId());
        Quadra quadra = travarQuadra(horario.getQuadraId());

        validarRegras(dto, horario, quadra, id);

        entity.setUsuario(buscarUsuario(usuarioId));
        entity.setHorario(horario);
        entity.setDataAgendamento(dto.dataAgendamento());
        entity.setHoraInicio(dto.horaInicio());
        entity.setHoraFim(dto.horaFim());
        entity.setValorTotal(calcularValor(quadra, dto));
        entity.setObservacao(dto.observacao());
        entity.setAtualizadoEm(OffsetDateTime.now());

        return AgendamentoResponseDTO.fromEntity(agendamentoRepository.save(entity));
    }

    /**
     * ADMIN ou gestor do estabelecimento da reserva.
     * Só aceita as mudanças previstas no ciclo de vida (StatusAgendamento.podeIrPara):
     *   PENDENTE -> CONFIRMADO/CANCELADO, CONFIRMADO -> CONCLUIDO/CANCELADO.
     */
    @Transactional
    public AgendamentoResponseDTO atualizarStatus(UUID id, StatusAgendamento novoStatus) {
        Agendamento entity = buscarEntidade(id);
        usuarioLogado.verificarGestor(estabelecimentoDe(entity));
        exigirTransicao(entity, novoStatus);

        if (novoStatus == StatusAgendamento.CONCLUIDO && !entity.jaTerminou(agora())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A reserva só pode ser concluída depois do horário de término ("
                            + entity.getDataAgendamento() + " às " + entity.getHoraFim() + ")");
        }

        if (novoStatus == StatusAgendamento.CANCELADO) {
            aplicarCancelamento(entity);
        } else {
            entity.setStatus(novoStatus);
            entity.setAtualizadoEm(OffsetDateTime.now());
        }

        log.info("Agendamento {} alterado para {}", id, novoStatus);
        return AgendamentoResponseDTO.fromEntity(agendamentoRepository.save(entity));
    }

    /** Dono, gestor do estabelecimento ou ADMIN. Também cancela o pagamento, se ainda estiver pendente. */
    @Transactional
    public void cancelar(UUID id) {
        Agendamento entity = buscarEntidade(id);
        usuarioLogado.verificarDonoOuGestor(entity.getUsuario().getId(), estabelecimentoDe(entity));
        exigirTransicao(entity, StatusAgendamento.CANCELADO);

        aplicarCancelamento(entity);
        agendamentoRepository.save(entity);

        log.info("Agendamento {} cancelado pelo usuário {}", id, usuarioLogado.id());
    }

    /**
     * Encerramento automático (chamado pela tarefa agendada em AgendadorConfig).
     * Reservas cujo horário de término já passou:
     *   CONFIRMADO -> CONCLUIDO
     *   PENDENTE   -> CANCELADO (e o pagamento pendente também é cancelado)
     *
     * Não depende de usuário logado: roda "em nome do sistema".
     *
     * @return quantas reservas foram concluídas e quantas canceladas
     */
    @Transactional
    public ResultadoEncerramento encerrarAgendamentosPassados() {
        LocalDateTime agora = agora();
        int concluidos = 0;
        int cancelados = 0;

        for (Agendamento a : agendamentoRepository.findByStatusInAndDataAgendamentoLessThanEqual(EM_ABERTO, agora.toLocalDate())) {
            if (!a.jaTerminou(agora)) {
                continue; // é de hoje, mas ainda não terminou
            }
            if (a.getStatus() == StatusAgendamento.CONFIRMADO) {
                a.setStatus(StatusAgendamento.CONCLUIDO);
                a.setAtualizadoEm(OffsetDateTime.now());
                concluidos++;
            } else {
                aplicarCancelamento(a);
                cancelados++;
            }
        }

        if (concluidos + cancelados > 0) {
            log.info("Encerramento automático: {} reserva(s) concluída(s), {} pendente(s) cancelada(s)", concluidos, cancelados);
        }
        return new ResultadoEncerramento(concluidos, cancelados);
    }

    /** Resultado do encerramento automático (útil nos logs e nos testes). */
    public record ResultadoEncerramento(int concluidos, int cancelados) {}

    /** Somente ADMIN (regra no SecurityConfig). */
    @Transactional
    public void deletar(UUID id) {
        if (!agendamentoRepository.existsById(id)) {
            throw new EntityNotFoundException("Agendamento não encontrado: " + id);
        }
        agendamentoRepository.deleteById(id);
        agendamentoRepository.flush(); // força o erro de FK (409) aparecer aqui, se houver pagamento/avaliação
    }

    // ---------- métodos auxiliares ----------

    /** Define para quem é o agendamento e bloqueia CLIENTE agendando para outra pessoa. */
    private UUID resolverUsuario(UUID usuarioIdInformado) {
        UUID logado = usuarioLogado.id();
        if (usuarioIdInformado == null || usuarioIdInformado.equals(logado)) {
            return logado;
        }
        if (!usuarioLogado.isAdmin()) {
            throw new AccessDeniedException("Você só pode fazer agendamentos para você mesmo");
        }
        return usuarioIdInformado;
    }

    private void validarRegras(AgendamentoRequestDTO dto, Horario horario, Quadra quadra, UUID ignorarId) {
        if (!dto.horaFim().isAfter(dto.horaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A hora de fim deve ser posterior à hora de início");
        }

        // A data pode ser hoje, mas o horário não pode já ter começado
        if (!LocalDateTime.of(dto.dataAgendamento(), dto.horaInicio()).isAfter(agora())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível reservar um horário que já começou ou já passou");
        }

        if (!Boolean.TRUE.equals(horario.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este horário está inativo");
        }

        if (!Boolean.TRUE.equals(quadra.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A quadra \"" + quadra.getNome() + "\" está inativa e não aceita reservas");
        }

        boolean estabelecimentoAtivo = estabelecimentoRepository.findById(quadra.getEstabelecimentoId())
                .map(e -> Boolean.TRUE.equals(e.getAtivo()))
                .orElse(false);
        if (!estabelecimentoAtivo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este estabelecimento está inativo e não aceita reservas");
        }

        // DayOfWeek: 1 = segunda ... 7 = domingo  ->  % 7 deixa domingo = 0 (padrão da tabela horario)
        int diaDaData = dto.dataAgendamento().getDayOfWeek().getValue() % 7;
        if (diaDaData != horario.getDiaSemana()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data informada não cai no dia da semana deste horário (diaSemana = "
                            + horario.getDiaSemana() + ", 0 = domingo)");
        }

        if (dto.horaInicio().isBefore(horario.getHoraInicio()) || dto.horaFim().isAfter(horario.getHoraFim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O agendamento deve ficar dentro do horário disponível ("
                            + horario.getHoraInicio() + " às " + horario.getHoraFim() + ")");
        }

        if (agendamentoRepository.existeConflito(
                horario.getQuadraId(), dto.dataAgendamento(),
                dto.horaInicio(), dto.horaFim(), ignorarId,
                StatusAgendamento.CANCELADO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe um agendamento nessa quadra nesse horário");
        }
    }

    /** valor/hora da quadra x duração em horas (2 casas decimais). */
    private BigDecimal calcularValor(Quadra quadra, AgendamentoRequestDTO dto) {
        long minutos = Duration.between(dto.horaInicio(), dto.horaFim()).toMinutes();
        return quadra.getValorHora()
                .multiply(BigDecimal.valueOf(minutos))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /**
     * Trava a quadra (SELECT ... FOR UPDATE) e devolve a entidade.
     * Enquanto esta transação não terminar, outra reserva da mesma quadra espera.
     */
    private Quadra travarQuadra(UUID quadraId) {
        quadraRepository.travarParaReserva(quadraId)
                .orElseThrow(() -> new EntityNotFoundException("Quadra não encontrada: " + quadraId));
        return quadraRepository.findById(quadraId).orElseThrow();
    }

    /** 409 se a mudança de status não for permitida pelo ciclo de vida da reserva. */
    private void exigirTransicao(Agendamento agendamento, StatusAgendamento novo) {
        StatusAgendamento atual = agendamento.getStatus();
        if (atual == novo) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A reserva já está " + atual);
        }
        if (!atual.podeIrPara(novo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível mudar uma reserva de " + atual + " para " + novo
                            + (atual.isFinal() ? " (" + atual + " é um status final)" : ""));
        }
    }

    /** Marca a reserva como CANCELADA e cancela o pagamento, se ainda estiver pendente. */
    private void aplicarCancelamento(Agendamento agendamento) {
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setAtualizadoEm(OffsetDateTime.now());

        pagamentoRepository.findByAgendamentoId(agendamento.getId())
                .filter(p -> p.getStatus() == PagamentoStatus.PENDENTE)
                .ifPresent(p -> p.setStatus(PagamentoStatus.CANCELADO));
    }

    /** Agora, no fuso dos estabelecimentos (ver RelogioConfig). */
    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    /** Agendamento -> horário -> quadra -> estabelecimento. */
    private UUID estabelecimentoDe(Agendamento agendamento) {
        return quadraRepository.findById(agendamento.getHorario().getQuadraId())
                .map(Quadra::getEstabelecimentoId)
                .orElse(null);
    }

    private Agendamento buscarEntidade(UUID id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado: " + id));
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
