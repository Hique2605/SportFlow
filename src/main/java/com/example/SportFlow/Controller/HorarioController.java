package com.example.SportFlow.Controller;

import com.example.SportFlow.Entity.Horario;
import com.example.SportFlow.Service.HorarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    private final HorarioService service;

    public HorarioController(HorarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<Horario> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Horario buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Horario> criar(@RequestBody Horario horario) {
        Horario criado = service.criar(horario);

        return ResponseEntity
                .created(URI.create("/api/horarios/" + criado.getId()))
                .body(criado);
    }

    @PutMapping("/{id}")
    public Horario atualizar(
            @PathVariable UUID id,
            @RequestBody Horario horario
    ) {
        return service.atualizar(id, horario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}