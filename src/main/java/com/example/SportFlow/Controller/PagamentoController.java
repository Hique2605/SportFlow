package com.example.SportFlow.Controller;

import com.example.SportFlow.Dto.Pagamento.PagamentoRequest;
import com.example.SportFlow.Dto.Pagamento.PagamentoResponse;
import com.example.SportFlow.Dto.Pagamento.PagamentoUpdateRequest;
import com.example.SportFlow.Service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Pagamentos", description = "Cliente paga os próprios agendamentos; ADMIN confirma, altera e exclui.")
@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping
    public ResponseEntity<PagamentoResponse> criar(@Valid @RequestBody PagamentoRequest dto) {
        PagamentoResponse criado = pagamentoService.criar(dto);
        return ResponseEntity.created(URI.create("/pagamentos/" + criado.id())).body(criado);
    }

    @GetMapping
    public List<PagamentoResponse> listarTodos() {
        return pagamentoService.listarTodos();
    }

    @GetMapping("/{id}")
    public PagamentoResponse buscarPorId(@PathVariable UUID id) {
        return pagamentoService.buscarPorId(id);
    }

    @GetMapping("/agendamento/{agendamentoId}")
    public PagamentoResponse buscarPorAgendamento(@PathVariable UUID agendamentoId) {
        return pagamentoService.buscarPorAgendamento(agendamentoId);
    }

    @Operation(summary = "Altera valor/forma de um pagamento pendente (ADMIN)")
    @PutMapping("/{id}")
    public PagamentoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody PagamentoUpdateRequest dto) {
        return pagamentoService.atualizar(id, dto);
    }

    @Operation(summary = "Confirma o pagamento e o agendamento (ADMIN)")
    @PatchMapping("/{id}/confirmar")
    public PagamentoResponse confirmarPagamento(@PathVariable UUID id) {
        return pagamentoService.confirmarPagamento(id);
    }

    @PatchMapping("/{id}/cancelar")
    public PagamentoResponse cancelarPagamento(@PathVariable UUID id) {
        return pagamentoService.cancelarPagamento(id);
    }

    @Operation(summary = "Exclui um pagamento não confirmado (ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        pagamentoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
