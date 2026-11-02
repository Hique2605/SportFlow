package com.example.SportFlow.Controller;

import com.example.SportFlow.Dto.Pagamento.PagamentoResponse;
import com.example.SportFlow.Entity.Pagamento;
import com.example.SportFlow.Service.PagamentoService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    // CRIAR PAGAMENTO
    @PostMapping
    public ResponseEntity<Pagamento> criar(
            @RequestBody Pagamento pagamento) {

        Pagamento novoPagamento = pagamentoService.criar(pagamento);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(novoPagamento);
    }

    // LISTAR TODOS OS PAGAMENTOS
    @GetMapping
    public ResponseEntity<List<PagamentoResponse>> listarTodos() {

        return ResponseEntity.ok(
                pagamentoService.listarTodos()
        );
    }

    // BUSCAR PAGAMENTO POR ID
    @GetMapping("/{id}")
    public ResponseEntity<Pagamento> buscarPorId(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                pagamentoService.buscarPorId(id)
        );
    }

    // BUSCAR PAGAMENTO POR AGENDAMENTO
    @GetMapping("/agendamento/{agendamentoId}")
    public ResponseEntity<Pagamento> buscarPorAgendamento(
            @PathVariable UUID agendamentoId) {

        return ResponseEntity.ok(
                pagamentoService.buscarPorAgendamento(agendamentoId)
        );
    }

    // ATUALIZAR PAGAMENTO
    @PutMapping("/{id}")
    public ResponseEntity<Pagamento> atualizar(
            @PathVariable UUID id,
            @RequestBody Pagamento pagamento) {

        return ResponseEntity.ok(
                pagamentoService.atualizar(id, pagamento)
        );
    }

    // CONFIRMAR PAGAMENTO
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<Pagamento> confirmarPagamento(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                pagamentoService.confirmarPagamento(id)
        );
    }

    // CANCELAR PAGAMENTO
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Pagamento> cancelarPagamento(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                pagamentoService.cancelarPagamento(id)
        );
    }

    // EXCLUIR PAGAMENTO
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable UUID id) {

        pagamentoService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}