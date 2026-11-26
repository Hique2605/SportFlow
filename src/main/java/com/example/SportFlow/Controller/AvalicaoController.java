package com.example.SportFlow.Controller;

import com.example.SportFlow.Dto.Avaliacao.AvaliacaoRequest;
import com.example.SportFlow.Dto.Avaliacao.AvaliacaoResponse;
import com.example.SportFlow.Dto.Avaliacao.AvaliacaoUpdateRequest;
import com.example.SportFlow.Service.AvaliacaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Avaliações", description = "Avaliação (nota 1 a 5) de um agendamento, feita pelo dono do agendamento.")
@RestController
@RequestMapping("/avaliacao")
@RequiredArgsConstructor
public class AvalicaoController {

    private final AvaliacaoService avaliacaoService;

    @GetMapping
    public List<AvaliacaoResponse> listar() {
        return avaliacaoService.listar();
    }

    @GetMapping("/{id}")
    public AvaliacaoResponse buscar(@PathVariable UUID id) {
        return avaliacaoService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<AvaliacaoResponse> criar(@Valid @RequestBody AvaliacaoRequest dto) {
        AvaliacaoResponse criada = avaliacaoService.criar(dto);
        return ResponseEntity.created(URI.create("/avaliacao/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public AvaliacaoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AvaliacaoUpdateRequest dto) {
        return avaliacaoService.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        avaliacaoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
