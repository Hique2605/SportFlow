package com.example.SportFlow.Controller;

import com.example.SportFlow.Entity.AvaliacaoEntity;
import com.example.SportFlow.Repository.AvaliacaoRepository;
import com.example.SportFlow.Service.AvaliacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/avaliacao")
public class AvalicaoController {

    @Autowired
    private AvaliacaoService avaliacaoService;

    @GetMapping
    public List<AvaliacaoEntity> listar() {
        return avaliacaoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvaliacaoEntity> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(avaliacaoService.findById(id));
    }

    @PostMapping
    public AvaliacaoEntity criar(@RequestBody AvaliacaoEntity avaliacaoEntity) {
        return avaliacaoService.save(avaliacaoEntity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AvaliacaoEntity> atualizar( @RequestBody AvaliacaoEntity usuario) {
        try {
            return ResponseEntity.ok(avaliacaoService.update(usuario));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        avaliacaoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
