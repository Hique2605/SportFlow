package com.example.SportFlow.Controller;

import com.example.SportFlow.Dto.Estabelecimento.EstabelecimentoRequest;
import com.example.SportFlow.Dto.Estabelecimento.EstabelecimentoResponse;
import com.example.SportFlow.Service.EstabelecimentoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Estabelecimentos", description = "Leitura: qualquer usuário logado. Escrita: ADMIN.")
@RestController
@RequestMapping("/api/estabelecimentos")
public class EstabelecimentoController {

    private final EstabelecimentoService service;

    public EstabelecimentoController(EstabelecimentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<EstabelecimentoResponse> listar() {
        return service.listar().stream().map(EstabelecimentoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EstabelecimentoResponse buscar(@PathVariable UUID id) {
        return EstabelecimentoResponse.from(service.buscar(id));
    }

    @PostMapping
    public ResponseEntity<EstabelecimentoResponse> criar(@Valid @RequestBody EstabelecimentoRequest dto) {
        EstabelecimentoResponse criado = EstabelecimentoResponse.from(service.criar(dto.toEntity()));
        return ResponseEntity.created(URI.create("/api/estabelecimentos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public EstabelecimentoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody EstabelecimentoRequest dto) {
        return EstabelecimentoResponse.from(service.atualizar(id, dto.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
