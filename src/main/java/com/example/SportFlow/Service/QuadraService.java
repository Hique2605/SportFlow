package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Quadra;
import com.example.SportFlow.Repository.EsporteRepository;
import com.example.SportFlow.Repository.HorarioRepository;
import com.example.SportFlow.Repository.QuadraRepository;
import com.example.SportFlow.config.UsuarioLogado;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class QuadraService {

    private final QuadraRepository repository;
    private final EsporteRepository esporteRepository;
    private final HorarioRepository horarioRepository;
    private final UsuarioLogado usuarioLogado;

    public QuadraService(QuadraRepository repository,
                         EsporteRepository esporteRepository,
                         HorarioRepository horarioRepository,
                         UsuarioLogado usuarioLogado) {
        this.repository = repository;
        this.esporteRepository = esporteRepository;
        this.horarioRepository = horarioRepository;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional(readOnly = true)
    public List<Quadra> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Quadra buscar(UUID id) {
        return encontrar(id);
    }

    @Transactional
    public Quadra criar(Quadra dados) {
        validar(dados);
        // ADMIN ou gestor do estabelecimento onde a quadra vai ficar
        usuarioLogado.verificarGestor(dados.getEstabelecimentoId());

        Quadra quadra = new Quadra();
        preencher(quadra, dados);

        try {
            return repository.saveAndFlush(quadra);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estabelecimento inexistente ou dados inválidos",
                    exception
            );
        }
    }

    @Transactional
    public Quadra atualizar(UUID id, Quadra dados) {
        validar(dados);

        Quadra quadra = encontrar(id);
        // Precisa gerenciar o estabelecimento atual e, se trocar, o novo também
        usuarioLogado.verificarGestor(quadra.getEstabelecimentoId());
        usuarioLogado.verificarGestor(dados.getEstabelecimentoId());
        verificarEsportesEmUso(quadra, dados.getEsporteIds());

        preencher(quadra, dados);

        try {
            return repository.saveAndFlush(quadra);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estabelecimento inexistente ou dados inválidos",
                    exception
            );
        }
    }

    @Transactional
    public void excluir(UUID id) {
        Quadra quadra = encontrar(id);
        usuarioLogado.verificarGestor(quadra.getEstabelecimentoId());

        try {
            repository.delete(quadra);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A quadra não pode ser excluída porque possui registros relacionados",
                    exception
            );
        }
    }

    private Quadra encontrar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Quadra inexistente: " + id
                ));
    }

    private void validar(Quadra quadra) {
        if (quadra == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os dados da quadra são obrigatórios"
            );
        }

        if (quadra.getEstabelecimentoId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O estabelecimento é obrigatório"
            );
        }

        if (quadra.getNome() == null || quadra.getNome().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O nome da quadra é obrigatório"
            );
        }

        if (quadra.getEsporteIds() == null || quadra.getEsporteIds().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe pelo menos um esporte que a quadra aceita"
            );
        }

        for (UUID esporteId : quadra.getEsporteIds()) {
            if (!esporteRepository.existsById(esporteId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Esporte inexistente: " + esporteId
                );
            }
        }

        if (quadra.getValorHora() == null ||
                quadra.getValorHora().signum() < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O valor por hora deve ser maior ou igual a zero"
            );
        }
    }

    /** Não deixa tirar da quadra um esporte que ainda tem horários cadastrados. */
    private void verificarEsportesEmUso(Quadra quadra, Set<UUID> novosEsportes) {
        boolean algumEmUso = horarioRepository.findByQuadraId(quadra.getId()).stream()
                .anyMatch(h -> !novosEsportes.contains(h.getEsporteId()));
        if (algumEmUso) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Existem horários desta quadra com um esporte que você está removendo. "
                            + "Altere ou exclua esses horários antes."
            );
        }
    }

    private void preencher(Quadra destino, Quadra origem) {
        destino.setEstabelecimentoId(origem.getEstabelecimentoId());
        destino.setNome(origem.getNome().trim());
        destino.setDescricao(origem.getDescricao());
        destino.setValorHora(origem.getValorHora());
        destino.setEsporteIds(origem.getEsporteIds());

        if (origem.getAtivo() != null) {
            destino.setAtivo(origem.getAtivo());
        }
    }
}