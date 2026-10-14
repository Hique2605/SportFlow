package com.example.SportFlow.Controller;

import com.example.SportFlow.Service.HorarioService;
import com.example.SportFlow.dto.request.HorarioRequest;
import com.example.SportFlow.dto.response.HorarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/horarios")
@RequiredArgsConstructor
public class HorarioController {

    private final HorarioService service;

    @GetMapping
    public List<HorarioResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public HorarioResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<HorarioResponse> criar(@Valid @RequestBody HorarioRequest request) {
        HorarioResponse criado = service.criar(request);
        return ResponseEntity.created(URI.create("/api/horarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public HorarioResponse atualizar(@PathVariable UUID id, @Valid @RequestBody HorarioRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
