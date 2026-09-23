package com.example.SportFlow.Controller;

import com.example.SportFlow.Entity.Esporte;
import com.example.SportFlow.Service.EsporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/esportes")
public class EsporteController {

    private final EsporteService service;

    public EsporteController(EsporteService service) {
        this.service = service;
    }

    @GetMapping
    public List<Esporte> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Esporte buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Esporte> criar(@RequestBody Esporte esporte) {
        Esporte criado = service.criar(esporte);

        return ResponseEntity
                .created(URI.create("/api/esportes/" + criado.getId()))
                .body(criado);
    }

    @PutMapping("/{id}")
    public Esporte atualizar(
            @PathVariable UUID id,
            @RequestBody Esporte esporte
    ) {
        return service.atualizar(id, esporte);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}