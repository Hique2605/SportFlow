package com.example.SportFlow.Controller;

import com.example.SportFlow.Dto.Esporte.EsporteRequest;
import com.example.SportFlow.Dto.Esporte.EsporteResponse;
import com.example.SportFlow.Service.EsporteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Esportes", description = "Leitura: qualquer usuário logado. Escrita: ADMIN.")
@RestController
@RequestMapping("/api/esportes")
public class EsporteController {

    private final EsporteService service;

    public EsporteController(EsporteService service) {
        this.service = service;
    }

    @GetMapping
    public List<EsporteResponse> listar() {
        return service.listar().stream().map(EsporteResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EsporteResponse buscar(@PathVariable UUID id) {
        return EsporteResponse.from(service.buscar(id));
    }

    @PostMapping
    public ResponseEntity<EsporteResponse> criar(@Valid @RequestBody EsporteRequest dto) {
        EsporteResponse criado = EsporteResponse.from(service.criar(dto.toEntity()));
        return ResponseEntity.created(URI.create("/api/esportes/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public EsporteResponse atualizar(@PathVariable UUID id, @Valid @RequestBody EsporteRequest dto) {
        return EsporteResponse.from(service.atualizar(id, dto.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
