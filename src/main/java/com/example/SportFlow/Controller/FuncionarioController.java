package com.example.SportFlow.Controller;

import com.example.SportFlow.Dto.Funcionario.FuncionarioRequest;
import com.example.SportFlow.Dto.Funcionario.FuncionarioResponse;
import com.example.SportFlow.Service.FuncionarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Funcionários", description = "Somente ADMIN.")
@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {

    private final FuncionarioService service;

    public FuncionarioController(FuncionarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<FuncionarioResponse> listar() {
        return service.listar().stream().map(FuncionarioResponse::from).toList();
    }

    @GetMapping("/{id}")
    public FuncionarioResponse buscar(@PathVariable UUID id) {
        return FuncionarioResponse.from(service.buscar(id));
    }

    @PostMapping
    public ResponseEntity<FuncionarioResponse> criar(@Valid @RequestBody FuncionarioRequest dto) {
        FuncionarioResponse criado = FuncionarioResponse.from(service.criar(dto.toEntity()));
        return ResponseEntity.created(URI.create("/api/funcionarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public FuncionarioResponse atualizar(@PathVariable UUID id, @Valid @RequestBody FuncionarioRequest dto) {
        return FuncionarioResponse.from(service.atualizar(id, dto.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
