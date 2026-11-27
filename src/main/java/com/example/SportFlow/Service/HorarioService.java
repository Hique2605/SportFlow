package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Horario;
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
import java.util.UUID;

@Service
public class HorarioService {

    private final HorarioRepository repository;
    private final QuadraRepository quadraRepository;
    private final EsporteRepository esporteRepository;
    private final UsuarioLogado usuarioLogado;

    public HorarioService(
            HorarioRepository repository,
            QuadraRepository quadraRepository,
            EsporteRepository esporteRepository,
            UsuarioLogado usuarioLogado
    ) {
        this.repository = repository;
        this.quadraRepository = quadraRepository;
        this.esporteRepository = esporteRepository;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional(readOnly = true)
    public List<Horario> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Horario buscar(UUID id) {
        return encontrar(id);
    }

    @Transactional
    public Horario criar(Horario dados) {
        validar(dados);
        verificarGestorDaQuadra(dados.getQuadraId());
        verificarConflito(dados, null);

        Horario horario = new Horario();
        preencher(horario, dados);

        return repository.saveAndFlush(horario);
    }

    @Transactional
    public Horario atualizar(UUID id, Horario dados) {
        Horario horario = encontrar(id);

        validar(dados);
        verificarGestorDaQuadra(horario.getQuadraId());   // quadra atual
        verificarGestorDaQuadra(dados.getQuadraId());     // quadra nova (se mudou)
        verificarConflito(dados, id);
        preencher(horario, dados);

        return repository.saveAndFlush(horario);
    }

    @Transactional
    public void excluir(UUID id) {
        Horario horario = encontrar(id);
        verificarGestorDaQuadra(horario.getQuadraId());

        try {
            repository.delete(horario);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O horário não pode ser excluído porque possui registros relacionados",
                    exception
            );
        }
    }

    private Horario encontrar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Horário inexistente: " + id
                ));
    }

    private void validar(Horario horario) {
        if (horario == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os dados do horário são obrigatórios"
            );
        }

        if (horario.getQuadraId() == null ||
                !quadraRepository.existsById(horario.getQuadraId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quadra inexistente: " + horario.getQuadraId()
            );
        }

        if (horario.getEsporteId() == null ||
                !esporteRepository.existsById(horario.getEsporteId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Esporte inexistente: " + horario.getEsporteId()
            );
        }

        // A quadra precisa aceitar o esporte do horário (lista da quadra, migration V1.5)
        Quadra quadra = quadraRepository.findById(horario.getQuadraId()).orElseThrow();
        if (!quadra.getEsporteIds().contains(horario.getEsporteId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A quadra \"" + quadra.getNome() + "\" não aceita este esporte. "
                            + "Edite a quadra para incluir o esporte ou escolha outro."
            );
        }

        if (horario.getDiaSemana() == null ||
                horario.getDiaSemana() < 0 ||
                horario.getDiaSemana() > 6) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O dia da semana deve estar entre 0 e 6"
            );
        }

        if (horario.getHoraInicio() == null ||
                horario.getHoraFim() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "As horas de início e fim são obrigatórias"
            );
        }

        if (!horario.getHoraFim().isAfter(horario.getHoraInicio())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A hora final deve ser posterior à hora inicial"
            );
        }
    }

    /**
     * Impede dois horários da mesma quadra, no mesmo dia, com faixas que se sobrepõem
     * (ex.: 08:00–10:00 e 09:00–11:00, ou dois 08:00–10:00 iguais).
     * idIgnorado: na atualização, o próprio horário não conta como conflito.
     */
    /** ADMIN ou gestor do estabelecimento dono da quadra. */
    private void verificarGestorDaQuadra(UUID quadraId) {
        Quadra quadra = quadraRepository.findById(quadraId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quadra inexistente: " + quadraId));
        usuarioLogado.verificarGestor(quadra.getEstabelecimentoId());
    }

    private void verificarConflito(Horario novo, UUID idIgnorado) {
        boolean conflita = repository.findAll().stream()
                .filter(h -> idIgnorado == null || !h.getId().equals(idIgnorado))
                .filter(h -> h.getQuadraId().equals(novo.getQuadraId()))
                .filter(h -> h.getDiaSemana().equals(novo.getDiaSemana()))
                .anyMatch(h -> novo.getHoraInicio().isBefore(h.getHoraFim())
                        && h.getHoraInicio().isBefore(novo.getHoraFim()));

        if (conflita) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um horário nesta quadra, neste dia, que se sobrepõe a este intervalo"
            );
        }
    }

    private void preencher(Horario destino, Horario origem) {
        destino.setQuadraId(origem.getQuadraId());
        destino.setEsporteId(origem.getEsporteId());
        destino.setDiaSemana(origem.getDiaSemana());
        destino.setHoraInicio(origem.getHoraInicio());
        destino.setHoraFim(origem.getHoraFim());

        if (origem.getAtivo() != null) {
            destino.setAtivo(origem.getAtivo());
        }
    }
}