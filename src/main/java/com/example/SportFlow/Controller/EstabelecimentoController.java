package com.example.SportFlow.Controller;

import com.example.SportFlow.Service.EstabelecimentoService;
import com.example.SportFlow.dto.request.EstabelecimentoRequest;
import com.example.SportFlow.dto.response.EstabelecimentoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/estabelecimentos")
@RequiredArgsConstructor
public class EstabelecimentoController {

    private final EstabelecimentoService service;

    @GetMapping
    public List<EstabelecimentoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public EstabelecimentoResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<EstabelecimentoResponse> criar(
            @Valid @RequestBody EstabelecimentoRequest request
    ) {
        EstabelecimentoResponse criado = service.criar(request);
        return ResponseEntity.created(URI.create("/api/estabelecimentos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public EstabelecimentoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EstabelecimentoRequest request
    ) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
