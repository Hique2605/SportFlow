package com.example.SportFlow.Controller;

import com.example.SportFlow.Entity.Estabelecimento;
import com.example.SportFlow.Service.EstabelecimentoService;
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

    @PostMapping
    public ResponseEntity<Estabelecimento> criar(
            @RequestBody Estabelecimento dados
    ) {
        Estabelecimento criado = service.criar(dados);

        return ResponseEntity
                .created(URI.create(
                        "/api/estabelecimentos/" + criado.getId()
                ))
                .body(criado);
    }

    @GetMapping
    public List<Estabelecimento> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Estabelecimento buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public Estabelecimento atualizar(
            @PathVariable UUID id,
            @RequestBody Estabelecimento dados
    ) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}