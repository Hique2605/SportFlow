package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Esporte;
import com.example.SportFlow.Repository.EsporteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class EsporteService {

    private final EsporteRepository repository;

    public EsporteService(EsporteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Esporte> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Esporte buscar(UUID id) {
        return encontrar(id);
    }

    @Transactional
    public Esporte criar(Esporte dados) {
        validar(dados);

        Esporte esporte = new Esporte();
        preencher(esporte, dados);

        return repository.saveAndFlush(esporte);
    }

    @Transactional
    public Esporte atualizar(UUID id, Esporte dados) {
        validar(dados);

        Esporte esporte = encontrar(id);
        preencher(esporte, dados);

        return repository.saveAndFlush(esporte);
    }

    @Transactional
    public void excluir(UUID id) {
        Esporte esporte = encontrar(id);
        repository.delete(esporte);
        repository.flush();
    }

    private Esporte encontrar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Esporte inexistente: " + id
                ));
    }

    private void validar(Esporte esporte) {
        if (esporte == null ||
                esporte.getNome() == null ||
                esporte.getNome().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O nome do esporte é obrigatório"
            );
        }
    }

    private void preencher(Esporte destino, Esporte origem) {
        destino.setNome(origem.getNome().trim());
        destino.setDescricao(origem.getDescricao());
    }
}