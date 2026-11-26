package com.example.SportFlow.Controller;

import com.example.SportFlow.Dto.Quadra.QuadraRequest;
import com.example.SportFlow.Dto.Quadra.QuadraResponse;
import com.example.SportFlow.Service.QuadraService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Quadras", description = "Leitura: qualquer usuário logado. Escrita: ADMIN.")
@RestController
@RequestMapping("/api/quadras")
public class QuadraController {

    private final QuadraService service;

    public QuadraController(QuadraService service) {
        this.service = service;
    }

    @GetMapping
    public List<QuadraResponse> listar() {
        return service.listar().stream().map(QuadraResponse::from).toList();
    }

    @GetMapping("/{id}")
    public QuadraResponse buscar(@PathVariable UUID id) {
        return QuadraResponse.from(service.buscar(id));
    }

    @PostMapping
    public ResponseEntity<QuadraResponse> criar(@Valid @RequestBody QuadraRequest dto) {
        QuadraResponse criado = QuadraResponse.from(service.criar(dto.toEntity()));
        return ResponseEntity.created(URI.create("/api/quadras/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public QuadraResponse atualizar(@PathVariable UUID id, @Valid @RequestBody QuadraRequest dto) {
        return QuadraResponse.from(service.atualizar(id, dto.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
