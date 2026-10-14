package com.example.SportFlow.Controller;

import com.example.SportFlow.Service.EsporteService;
import com.example.SportFlow.dto.request.EsporteRequest;
import com.example.SportFlow.dto.response.EsporteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/esportes")
@RequiredArgsConstructor
public class EsporteController {

    private final EsporteService service;

    @GetMapping
    public List<EsporteResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public EsporteResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<EsporteResponse> criar(@Valid @RequestBody EsporteRequest request) {
        EsporteResponse criado = service.criar(request);
        return ResponseEntity.created(URI.create("/api/esportes/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public EsporteResponse atualizar(@PathVariable UUID id, @Valid @RequestBody EsporteRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
