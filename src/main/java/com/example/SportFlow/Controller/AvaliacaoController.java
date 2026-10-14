package com.example.SportFlow.Controller;

import com.example.SportFlow.Service.AvaliacaoService;
import com.example.SportFlow.dto.request.AvaliacaoRequest;
import com.example.SportFlow.dto.response.AvaliacaoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {

    private final AvaliacaoService service;

    @GetMapping
    public List<AvaliacaoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public AvaliacaoResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<AvaliacaoResponse> criar(@Valid @RequestBody AvaliacaoRequest request) {
        AvaliacaoResponse criado = service.criar(request);
        return ResponseEntity.created(URI.create("/api/avaliacoes/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public AvaliacaoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AvaliacaoRequest request
    ) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
