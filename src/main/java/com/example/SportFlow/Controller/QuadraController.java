package com.example.SportFlow.Controller;

import com.example.SportFlow.Service.QuadraService;
import com.example.SportFlow.dto.request.QuadraRequest;
import com.example.SportFlow.dto.response.QuadraResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quadras")
@RequiredArgsConstructor
public class QuadraController {

    private final QuadraService service;

    @GetMapping
    public List<QuadraResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public QuadraResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<QuadraResponse> criar(@Valid @RequestBody QuadraRequest request) {
        QuadraResponse criado = service.criar(request);
        return ResponseEntity.created(URI.create("/api/quadras/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public QuadraResponse atualizar(@PathVariable UUID id, @Valid @RequestBody QuadraRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
