package com.example.SportFlow.Controller;

import com.example.SportFlow.Dto.Horario.HorarioRequest;
import com.example.SportFlow.Dto.Horario.HorarioResponse;
import com.example.SportFlow.Service.HorarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Horários", description = "Leitura: qualquer usuário logado. Escrita: ADMIN.")
@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    private final HorarioService service;

    public HorarioController(HorarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<HorarioResponse> listar() {
        return service.listar().stream().map(HorarioResponse::from).toList();
    }

    @GetMapping("/{id}")
    public HorarioResponse buscar(@PathVariable UUID id) {
        return HorarioResponse.from(service.buscar(id));
    }

    @PostMapping
    public ResponseEntity<HorarioResponse> criar(@Valid @RequestBody HorarioRequest dto) {
        HorarioResponse criado = HorarioResponse.from(service.criar(dto.toEntity()));
        return ResponseEntity.created(URI.create("/api/horarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public HorarioResponse atualizar(@PathVariable UUID id, @Valid @RequestBody HorarioRequest dto) {
        return HorarioResponse.from(service.atualizar(id, dto.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
