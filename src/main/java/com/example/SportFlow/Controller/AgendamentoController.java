package com.example.SportFlow.Controller;

import com.example.SportFlow.Dto.AgendamentoDto.AgendamentoRequestDTO;
import com.example.SportFlow.Dto.AgendamentoDto.AgendamentoResponseDTO;
import com.example.SportFlow.Dto.AgendamentoDto.AgendamentoStatusDTO;
import com.example.SportFlow.Service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService service;

    @PostMapping
    public ResponseEntity<AgendamentoResponseDTO> criar(@Valid @RequestBody AgendamentoRequestDTO dto) {
        AgendamentoResponseDTO criado = service.criar(dto);
        return ResponseEntity.created(URI.create("/agendamentos/" + criado.id())).body(criado);
    }

    @GetMapping
    public List<AgendamentoResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public AgendamentoResponseDTO buscarPorId(@PathVariable UUID id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<AgendamentoResponseDTO> listarPorUsuario(@PathVariable UUID usuarioId) {
        return service.listarPorUsuario(usuarioId);
    }

    @PutMapping("/{id}")
    public AgendamentoResponseDTO atualizar(@PathVariable UUID id,
                                            @Valid @RequestBody AgendamentoRequestDTO dto) {
        return service.atualizar(id, dto);
    }

    @PatchMapping("/{id}/status")
    public AgendamentoResponseDTO atualizarStatus(@PathVariable UUID id,
                                                  @Valid @RequestBody AgendamentoStatusDTO dto) {
        return service.atualizarStatus(id, dto.status());
    }

    @PatchMapping("/{id}/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable UUID id) {
        service.cancelar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        service.deletar(id);
    }
}
