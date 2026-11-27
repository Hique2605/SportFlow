package com.example.SportFlow.Service;

import com.example.SportFlow.Dto.Pagamento.PagamentoRequest;
import com.example.SportFlow.Dto.Pagamento.PagamentoResponse;
import com.example.SportFlow.Dto.Pagamento.PagamentoUpdateRequest;
import com.example.SportFlow.Entity.Agendamento;
import com.example.SportFlow.Entity.Pagamento;
import com.example.SportFlow.Entity.Quadra;
import com.example.SportFlow.Enum.PagamentoStatus;
import com.example.SportFlow.Enum.StatusAgendamento;
import com.example.SportFlow.Repository.AgendamentoRepository;
import com.example.SportFlow.Repository.PagamentoRepository;
import com.example.SportFlow.Repository.QuadraRepository;
import com.example.SportFlow.config.UsuarioLogado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Todos os métodos devolvem DTO montado DENTRO da transação.
 * Antes a entidade era devolvida ao controller e o Jackson tentava ler o
 * agendamento (LAZY) depois da transação fechada, gerando erro 500.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final QuadraRepository quadraRepository;
    private final UsuarioLogado usuarioLogado;

    @Transactional
    public PagamentoResponse criar(PagamentoRequest dto) {
        Agendamento agendamento = agendamentoRepository.findById(dto.agendamentoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado"));

        usuarioLogado.verificarDonoOuAdmin(agendamento.getUsuario().getId());

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível pagar um agendamento cancelado");
        }

        if (pagamentoRepository.existsByAgendamentoId(dto.agendamentoId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este agendamento já possui um pagamento");
        }

        // O valor vem da reserva; o cliente não escolhe quanto paga
        BigDecimal valor = exigirValorDaReserva(dto.valor(), agendamento);

        Pagamento novo = Pagamento.builder()
                .agendamento(agendamento)
                .valor(valor)
                .forma(dto.forma().trim())
                .status(PagamentoStatus.PENDENTE)
                .criadoEm(OffsetDateTime.now())
                .build();

        Pagamento salvo = pagamentoRepository.save(novo);
        log.info("Pagamento {} criado para o agendamento {} ({} via {})",
                salvo.getId(), agendamento.getId(), valor, dto.forma());
        return PagamentoResponse.from(salvo);
    }

    /**
     * ADMIN vê todos.
     * Os demais veem os dos próprios agendamentos + os dos estabelecimentos que gerenciam.
     */
    @Transactional(readOnly = true)
    public List<PagamentoResponse> listarTodos() {
        if (usuarioLogado.isAdmin()) {
            return pagamentoRepository.findAll().stream().map(PagamentoResponse::from).toList();
        }

        UUID logado = usuarioLogado.id();
        Set<UUID> gerenciados = usuarioLogado.estabelecimentosGerenciados();
        // quadraId -> estabelecimentoId (uma consulta só, em vez de uma por pagamento)
        Map<UUID, UUID> estabelecimentoDaQuadra = quadraRepository.findAll().stream()
                .collect(Collectors.toMap(Quadra::getId, Quadra::getEstabelecimentoId));

        return pagamentoRepository.findAll().stream()
                .filter(p -> p.getAgendamento().getUsuario().getId().equals(logado)
                        || gerenciados.contains(estabelecimentoDaQuadra.get(p.getAgendamento().getHorario().getQuadraId())))
                .map(PagamentoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagamentoResponse buscarPorId(UUID id) {
        Pagamento pagamento = encontrar(id);
        verificarAcesso(pagamento);
        return PagamentoResponse.from(pagamento);
    }

    @Transactional(readOnly = true)
    public PagamentoResponse buscarPorAgendamento(UUID agendamentoId) {
        Pagamento pagamento = pagamentoRepository.findByAgendamentoId(agendamentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pagamento não encontrado para este agendamento"));
        verificarAcesso(pagamento);
        return PagamentoResponse.from(pagamento);
    }

    /** Somente ADMIN (regra no SecurityConfig). */
    @Transactional
    public PagamentoResponse atualizar(UUID id, PagamentoUpdateRequest dados) {
        Pagamento pagamento = encontrar(id);
        exigirPendente(pagamento, "alterados");

        pagamento.setValor(exigirValorDaReserva(dados.valor(), pagamento.getAgendamento()));
        pagamento.setForma(dados.forma().trim());

        return PagamentoResponse.from(pagamentoRepository.save(pagamento));
    }

    /** ADMIN ou gestor do estabelecimento. Confirma o pagamento e o agendamento junto. */
    @Transactional
    public PagamentoResponse confirmarPagamento(UUID id) {
        Pagamento pagamento = encontrar(id);
        usuarioLogado.verificarGestor(estabelecimentoDo(pagamento));
        exigirPendente(pagamento, "confirmados");

        pagamento.setStatus(PagamentoStatus.PAGO);
        pagamento.setPagoEm(OffsetDateTime.now());

        Agendamento agendamento = pagamento.getAgendamento();
        if (agendamento.getStatus() == StatusAgendamento.PENDENTE) {
            agendamento.setStatus(StatusAgendamento.CONFIRMADO);
            agendamento.setAtualizadoEm(OffsetDateTime.now());
        }

        log.info("Pagamento {} confirmado; agendamento {} -> {}", id, agendamento.getId(), agendamento.getStatus());
        return PagamentoResponse.from(pagamentoRepository.save(pagamento));
    }

    /** Dono do agendamento, gestor do estabelecimento ou ADMIN. */
    @Transactional
    public PagamentoResponse cancelarPagamento(UUID id) {
        Pagamento pagamento = encontrar(id);
        verificarAcesso(pagamento);
        exigirPendente(pagamento, "cancelados");

        pagamento.setStatus(PagamentoStatus.CANCELADO);
        pagamento.setPagoEm(null);

        return PagamentoResponse.from(pagamentoRepository.save(pagamento));
    }

    /** Somente ADMIN. */
    @Transactional
    public void excluir(UUID id) {
        Pagamento pagamento = encontrar(id);

        if (pagamento.getStatus() == PagamentoStatus.PAGO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pagamentos confirmados não podem ser excluídos");
        }

        pagamentoRepository.delete(pagamento);
    }

    // ---------- auxiliares ----------

    private Pagamento encontrar(UUID id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento não encontrado"));
    }

    /** Dono do agendamento, gestor do estabelecimento ou ADMIN. */
    private void verificarAcesso(Pagamento pagamento) {
        usuarioLogado.verificarDonoOuGestor(pagamento.getAgendamento().getUsuario().getId(), estabelecimentoDo(pagamento));
    }

    /** Pagamento -> agendamento -> horário -> quadra -> estabelecimento. */
    private UUID estabelecimentoDo(Pagamento pagamento) {
        return quadraRepository.findById(pagamento.getAgendamento().getHorario().getQuadraId())
                .map(Quadra::getEstabelecimentoId)
                .orElse(null);
    }

    /**
     * O pagamento tem que cobrir exatamente o valor da reserva.
     * Sem valor informado -> usa o valorTotal. Valor diferente -> 400.
     */
    private BigDecimal exigirValorDaReserva(BigDecimal informado, Agendamento agendamento) {
        BigDecimal esperado = agendamento.getValorTotal();
        if (informado != null && informado.compareTo(esperado) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O valor do pagamento deve ser igual ao valor da reserva: R$ "
                            + esperado.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ','));
        }
        return esperado;
    }

    private void exigirPendente(Pagamento pagamento, String acao) {
        if (pagamento.getStatus() != PagamentoStatus.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente pagamentos pendentes podem ser " + acao);
        }
    }
}
