package com.example.SportFlow.Controller;

import com.example.SportFlow.Entity.Quadra;
import com.example.SportFlow.Service.QuadraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quadras")
public class QuadraController {

    private final QuadraService service;

    public QuadraController(QuadraService service) {
        this.service = service;
    }

    @GetMapping
    public List<Quadra> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Quadra buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Quadra> criar(@RequestBody Quadra quadra) {
        Quadra criada = service.criar(quadra);

        return ResponseEntity
                .created(URI.create("/api/quadras/" + criada.getId()))
                .body(criada);
    }

    @PutMapping("/{id}")
    public Quadra atualizar(
            @PathVariable UUID id,
            @RequestBody Quadra quadra
    ) {
        return service.atualizar(id, quadra);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}