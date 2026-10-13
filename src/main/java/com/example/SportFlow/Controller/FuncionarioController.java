package com.example.SportFlow.Controller;

import com.example.SportFlow.Entity.Funcionario;
import com.example.SportFlow.Service.FuncionarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {

    private final FuncionarioService service;

    @PostMapping
    public ResponseEntity<Funcionario> criar(
            @RequestBody Funcionario dados
    ) {
        Funcionario criado = service.criar(dados);

        return ResponseEntity
                .created(URI.create(
                        "/api/funcionarios/" + criado.getId()
                ))
                .body(criado);
    }

    @GetMapping
    public List<Funcionario> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Funcionario buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public Funcionario atualizar(
            @PathVariable UUID id,
            @RequestBody Funcionario dados
    ) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}