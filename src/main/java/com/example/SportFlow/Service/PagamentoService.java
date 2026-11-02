package com.example.SportFlow.Service;

import com.example.SportFlow.Dto.Pagamento.PagamentoResponse;
import com.example.SportFlow.Entity.Agendamento;
import com.example.SportFlow.Entity.Pagamento;
import com.example.SportFlow.Enum.PagamentoStatus;
import com.example.SportFlow.Repository.AgendamentoRepository;
import com.example.SportFlow.Repository.PagamentoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional
    public Pagamento criar(Pagamento pagamento) {

        if (pagamento.getAgendamento() == null ||
                pagamento.getAgendamento().getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Agendamento é obrigatório"
            );
        }

        if (pagamento.getValor() == null ||
                pagamento.getValor().compareTo(BigDecimal.ZERO) <= 0 ||
                pagamento.getValor().scale() > 2 ||
                pagamento.getValor().precision()
                        - pagamento.getValor().scale() > 8) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valor do pagamento inválido"
            );
        }

        if (pagamento.getForma() == null ||
                pagamento.getForma().isBlank() ||
                pagamento.getForma().length() > 30) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Forma de pagamento inválida"
            );
        }

        UUID agendamentoId = pagamento.getAgendamento().getId();

        Agendamento agendamento = agendamentoRepository
                .findById(agendamentoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Agendamento não encontrado"
                ));

        if (pagamentoRepository.existsByAgendamentoId(agendamentoId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este agendamento já possui um pagamento"
            );
        }

        Pagamento novoPagamento = Pagamento.builder()
                .agendamento(agendamento)
                .valor(pagamento.getValor())
                .forma(pagamento.getForma())
                .status(PagamentoStatus.PENDENTE)
                .criadoEm(OffsetDateTime.now())
                .build();

        return pagamentoRepository.save(novoPagamento);
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> listarTodos() {

        return pagamentoRepository.findAll()
                .stream()
                .map(this::converter)
                .toList();
    }

    @Transactional(readOnly = true)
    public Pagamento buscarPorId(UUID id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pagamento não encontrado"
                ));
    }

    @Transactional(readOnly = true)
    public Pagamento buscarPorAgendamento(UUID agendamentoId) {
        return pagamentoRepository.findByAgendamentoId(agendamentoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pagamento não encontrado para este agendamento"
                ));
    }

    @Transactional
    public Pagamento atualizar(UUID id, Pagamento dados) {

        Pagamento pagamento = buscarPorId(id);

        if (pagamento.getStatus() != PagamentoStatus.PENDENTE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Somente pagamentos pendentes podem ser alterados"
            );
        }

        if (dados.getValor() == null ||
                dados.getValor().compareTo(BigDecimal.ZERO) <= 0 ||
                dados.getValor().scale() > 2 ||
                dados.getValor().precision()
                        - dados.getValor().scale() > 8) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valor do pagamento inválido"
            );
        }

        if (dados.getForma() == null ||
                dados.getForma().isBlank() ||
                dados.getForma().length() > 30) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Forma de pagamento inválida"
            );
        }

        pagamento.setValor(dados.getValor());
        pagamento.setForma(dados.getForma());

        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public Pagamento confirmarPagamento(UUID id) {

        Pagamento pagamento = buscarPorId(id);

        if (pagamento.getStatus() != PagamentoStatus.PENDENTE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Somente pagamentos pendentes podem ser confirmados"
            );
        }

        pagamento.setStatus(PagamentoStatus.PAGO);
        pagamento.setPagoEm(OffsetDateTime.now());

        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public Pagamento cancelarPagamento(UUID id) {

        Pagamento pagamento = buscarPorId(id);

        if (pagamento.getStatus() != PagamentoStatus.PENDENTE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Somente pagamentos pendentes podem ser cancelados"
            );
        }

        pagamento.setStatus(PagamentoStatus.CANCELADO);
        pagamento.setPagoEm(null);

        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public void excluir(UUID id) {

        Pagamento pagamento = buscarPorId(id);

        if (pagamento.getStatus() == PagamentoStatus.PAGO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Pagamentos confirmados não podem ser excluídos"
            );
        }

        pagamentoRepository.delete(pagamento);
    }

    private PagamentoResponse converter(Pagamento pagamento) {

        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getAgendamento().getId(),
                pagamento.getValor(),
                pagamento.getForma(),
                pagamento.getStatus(),
                pagamento.getCriadoEm(),
                pagamento.getPagoEm()
        );
    }

}