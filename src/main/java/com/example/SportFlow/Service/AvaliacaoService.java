package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.AvaliacaoEntity;
import com.example.SportFlow.Repository.AvaliacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AvaliacaoService {

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    public AvaliacaoEntity findById(UUID id) {
        return avaliacaoRepository.findById(id).orElse(null);       //LANCAR EXCEPTION MINHA DPS
    }

    public List<AvaliacaoEntity> findAll() {
        return avaliacaoRepository.findAll();
    }

    public AvaliacaoEntity save(AvaliacaoEntity avaliacaoEntity) {
        return avaliacaoRepository.save(avaliacaoEntity);
    }

    public AvaliacaoEntity update(AvaliacaoEntity avaliacaoEntity) {

        AvaliacaoEntity avaliacaoExistente = avaliacaoRepository.findById(avaliacaoEntity.getId())
                                                                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada")); //LANCAR EXCEPTION MINHA DPS

        avaliacaoExistente.setNota(avaliacaoEntity.getNota());
        avaliacaoExistente.setComentario(avaliacaoEntity.getComentario());

        return avaliacaoRepository.save(avaliacaoExistente);
    }

    public void deleteById(UUID id) {
        avaliacaoRepository.deleteById(id);
    }

}
