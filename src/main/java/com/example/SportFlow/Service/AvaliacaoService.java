package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Avaliacao;
import com.example.SportFlow.Repository.AvaliacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AvaliacaoService {

    @Autowired
    private final AvaliacaoRepository avaliacaoRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public Avaliacao findById(UUID id) {
        return avaliacaoRepository.findById(id).orElse(null);       //LANCAR EXCEPTION MINHA DPS
    }

    public List<Avaliacao> findAll() {
        return avaliacaoRepository.findAll();
    }

    public Avaliacao save(Avaliacao avaliacao) {
        return avaliacaoRepository.save(avaliacao);
    }

    public Avaliacao update(Avaliacao avaliacao) {

        Avaliacao avaliacaoExistente = avaliacaoRepository.findById(avaliacao.getId())
                                                                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada")); //LANCAR EXCEPTION MINHA DPS

        avaliacaoExistente.setNota(avaliacao.getNota());
        avaliacaoExistente.setComentario(avaliacao.getComentario());

        return avaliacaoRepository.save(avaliacaoExistente);
    }

    public void deleteById(UUID id) {
        avaliacaoRepository.deleteById(id);
    }

}
