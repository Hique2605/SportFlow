package com.example.SportFlow.Service;

import com.example.SportFlow.Entity.Agendamento;
import com.example.SportFlow.Entity.Avaliacao;
import com.example.SportFlow.Entity.Esporte;
import com.example.SportFlow.Entity.Estabelecimento;
import com.example.SportFlow.Entity.Horario;
import com.example.SportFlow.Entity.Quadra;
import com.example.SportFlow.Enum.StatusAgendamento;
import com.example.SportFlow.Repository.AgendamentoRepository;
import com.example.SportFlow.Repository.AvaliacaoRepository;
import com.example.SportFlow.Repository.EsporteRepository;
import com.example.SportFlow.Repository.EstabelecimentoRepository;
import com.example.SportFlow.Repository.HorarioRepository;
import com.example.SportFlow.Repository.QuadraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Responde "quais horários estão LIVRES?" a partir dos dados reais do banco.
 *
 * É usada pelo assistente de IA (as ferramentas @Tool chamam estes métodos), mas
 * não depende de IA: é regra de negócio comum, por isso fica na camada Service.
 *
 * Um horário livre = horário ativo, de quadra e estabelecimento ativos, que ainda
 * não começou e sem reserva (não cancelada) na mesma quadra naquele intervalo.
 */
@Service
@RequiredArgsConstructor
public class DisponibilidadeService {

    /** Para a busca não varrer meses inteiros. */
    public static final int MAXIMO_DIAS = 14;

    private final EsporteRepository esporteRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final QuadraRepository quadraRepository;
    private final HorarioRepository horarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final Clock relogio;

    /** Filtros da busca. Todos opcionais (null = sem filtro). */
    public record Filtro(String esporte, LocalDate dataInicial, LocalDate dataFinal,
                         LocalTime horaMinima, LocalTime horaMaxima,
                         BigDecimal valorMaximo, String local, int limite) {}

    /** Um horário livre numa data específica. */
    public record OpcaoLivre(UUID horarioId, LocalDate data, LocalTime horaInicio, LocalTime horaFim,
                             UUID estabelecimentoId, String estabelecimentoNome, String bairro, String cidade,
                             UUID quadraId, String quadraNome, String esporteNome,
                             BigDecimal valor, Double notaMedia) {}

    /** Resumo de um estabelecimento para o assistente. */
    public record ResumoEstabelecimento(String nome, String bairro, String cidade, List<String> esportes,
                                        BigDecimal menorValorHora, BigDecimal maiorValorHora,
                                        Double notaMedia, long avaliacoes) {}

    /** Reserva futura do usuário, já com nomes em vez de ids. */
    public record ReservaResumo(LocalDate data, LocalTime horaInicio, LocalTime horaFim, String estabelecimentoNome,
                                String quadraNome, String esporteNome, StatusAgendamento status, BigDecimal valor) {}

    @Transactional(readOnly = true)
    public List<String> nomesDosEsportes() {
        return esporteRepository.findAll().stream().map(Esporte::getNome).sorted().toList();
    }

    @Transactional(readOnly = true)
    public List<OpcaoLivre> buscar(Filtro filtro) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate inicio = filtro.dataInicial() != null && filtro.dataInicial().isAfter(agora.toLocalDate())
                ? filtro.dataInicial() : agora.toLocalDate();
        LocalDate fim = filtro.dataFinal() != null ? filtro.dataFinal() : inicio.plusDays(6);
        if (fim.isAfter(inicio.plusDays(MAXIMO_DIAS - 1))) fim = inicio.plusDays(MAXIMO_DIAS - 1);

        Catalogo c = carregarCatalogo();
        String esporteBuscado = normalizar(filtro.esporte());
        String localBuscado = normalizar(filtro.local());

        List<OpcaoLivre> opcoes = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            int diaSemana = dia.getDayOfWeek().getValue() % 7; // 0 = domingo, igual à tabela horario

            for (Horario h : c.horarios) {
                if (!Boolean.TRUE.equals(h.getAtivo()) || h.getDiaSemana() != diaSemana) continue;

                Quadra q = c.quadras.get(h.getQuadraId());
                if (q == null || !Boolean.TRUE.equals(q.getAtivo())) continue;
                Estabelecimento e = c.estabelecimentos.get(q.getEstabelecimentoId());
                if (e == null || !Boolean.TRUE.equals(e.getAtivo())) continue;

                String esporte = c.esportes.getOrDefault(h.getEsporteId(), "");
                if (esporteBuscado != null && !normalizar(esporte).contains(esporteBuscado)) continue;
                if (localBuscado != null && !contemAlgum(localBuscado, e.getNome(), e.getBairro(), e.getCidade())) continue;

                if (filtro.horaMinima() != null && h.getHoraInicio().isBefore(filtro.horaMinima())) continue;
                if (filtro.horaMaxima() != null && !h.getHoraInicio().isBefore(filtro.horaMaxima())) continue;
                if (!LocalDateTime.of(dia, h.getHoraInicio()).isAfter(agora)) continue;

                BigDecimal valor = valorDe(q, h);
                if (filtro.valorMaximo() != null && valor.compareTo(filtro.valorMaximo()) > 0) continue;

                boolean ocupado = agendamentoRepository.existeConflito(
                        q.getId(), dia, h.getHoraInicio(), h.getHoraFim(), null, StatusAgendamento.CANCELADO);
                if (ocupado) continue;

                opcoes.add(new OpcaoLivre(h.getId(), dia, h.getHoraInicio(), h.getHoraFim(),
                        e.getId(), e.getNome(), e.getBairro(), e.getCidade(),
                        q.getId(), q.getNome(), esporte, valor, c.notas.get(e.getId())));
            }
        }

        int limite = filtro.limite() > 0 ? filtro.limite() : 8;
        return opcoes.stream()
                .sorted(Comparator.comparing(OpcaoLivre::data)
                        .thenComparing(OpcaoLivre::horaInicio)
                        .thenComparing(OpcaoLivre::valor))
                .limit(limite)
                .toList();
    }

    /** Confere de novo, na hora de mostrar, se a opção continua livre. */
    @Transactional(readOnly = true)
    public boolean continuaLivre(OpcaoLivre o) {
        return LocalDateTime.of(o.data(), o.horaInicio()).isAfter(LocalDateTime.now(relogio))
                && !agendamentoRepository.existeConflito(o.quadraId(), o.data(), o.horaInicio(), o.horaFim(),
                        null, StatusAgendamento.CANCELADO);
    }

    @Transactional(readOnly = true)
    public List<ResumoEstabelecimento> resumoDosEstabelecimentos() {
        Catalogo c = carregarCatalogo();
        Map<UUID, Long> qtdAvaliacoes = c.contagemAvaliacoes;

        List<ResumoEstabelecimento> lista = new ArrayList<>();
        for (Estabelecimento e : c.estabelecimentos.values()) {
            if (!Boolean.TRUE.equals(e.getAtivo())) continue;
            List<Quadra> quadras = c.quadras.values().stream()
                    .filter(q -> q.getEstabelecimentoId().equals(e.getId()) && Boolean.TRUE.equals(q.getAtivo()))
                    .toList();
            List<String> esportes = quadras.stream()
                    .flatMap(q -> q.getEsporteIds().stream())
                    .map(id -> c.esportes.getOrDefault(id, "?"))
                    .distinct().sorted().toList();
            BigDecimal menor = quadras.stream().map(Quadra::getValorHora).min(BigDecimal::compareTo).orElse(null);
            BigDecimal maior = quadras.stream().map(Quadra::getValorHora).max(BigDecimal::compareTo).orElse(null);
            lista.add(new ResumoEstabelecimento(e.getNome(), e.getBairro(), e.getCidade(), esportes, menor, maior,
                    c.notas.get(e.getId()), qtdAvaliacoes.getOrDefault(e.getId(), 0L)));
        }
        lista.sort(Comparator.comparing(ResumoEstabelecimento::nome));
        return lista;
    }

    @Transactional(readOnly = true)
    public List<ReservaResumo> proximasReservas(UUID usuarioId) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        Catalogo c = carregarCatalogo();
        Map<UUID, Horario> horarios = c.horarios.stream().collect(Collectors.toMap(Horario::getId, Function.identity()));

        return agendamentoRepository.findByUsuarioId(usuarioId).stream()
                .filter(a -> a.getStatus() == StatusAgendamento.PENDENTE || a.getStatus() == StatusAgendamento.CONFIRMADO)
                .filter(a -> !a.jaTerminou(agora))
                .sorted(Comparator.comparing(Agendamento::momentoInicio))
                .map(a -> {
                    Horario h = horarios.get(a.getHorario().getId());
                    Quadra q = h == null ? null : c.quadras.get(h.getQuadraId());
                    Estabelecimento e = q == null ? null : c.estabelecimentos.get(q.getEstabelecimentoId());
                    return new ReservaResumo(a.getDataAgendamento(), a.getHoraInicio(), a.getHoraFim(),
                            e == null ? "?" : e.getNome(), q == null ? "?" : q.getNome(),
                            h == null ? "?" : c.esportes.getOrDefault(h.getEsporteId(), "?"),
                            a.getStatus(), a.getValorTotal());
                })
                .toList();
    }

    // ------------------------------------------------------------------ auxiliares

    /** Tudo o que a busca precisa, carregado uma vez (poucas consultas em vez de uma por horário). */
    private Catalogo carregarCatalogo() {
        Catalogo c = new Catalogo();
        esporteRepository.findAll().forEach(e -> c.esportes.put(e.getId(), e.getNome()));
        estabelecimentoRepository.findAll().forEach(e -> c.estabelecimentos.put(e.getId(), e));
        quadraRepository.findAll().forEach(q -> c.quadras.put(q.getId(), q));
        c.horarios = horarioRepository.findAll();

        // Nota média por estabelecimento: avaliação -> agendamento -> horário -> quadra -> estabelecimento
        Map<UUID, Horario> horarioPorId = c.horarios.stream().collect(Collectors.toMap(Horario::getId, Function.identity()));
        Map<UUID, List<Short>> notas = new HashMap<>();
        for (Avaliacao av : avaliacaoRepository.findAll()) {
            Horario h = horarioPorId.get(av.getAgendamento().getHorario().getId());
            Quadra q = h == null ? null : c.quadras.get(h.getQuadraId());
            if (q != null) notas.computeIfAbsent(q.getEstabelecimentoId(), k -> new ArrayList<>()).add(av.getNota());
        }
        notas.forEach((estab, lista) -> {
            double media = lista.stream().mapToInt(Short::intValue).average().orElse(0);
            c.notas.put(estab, Math.round(media * 10) / 10.0);
            c.contagemAvaliacoes.put(estab, (long) lista.size());
        });
        return c;
    }

    private static class Catalogo {
        final Map<UUID, String> esportes = new HashMap<>();
        final Map<UUID, Estabelecimento> estabelecimentos = new HashMap<>();
        final Map<UUID, Quadra> quadras = new HashMap<>();
        final Map<UUID, Double> notas = new HashMap<>();
        final Map<UUID, Long> contagemAvaliacoes = new HashMap<>();
        List<Horario> horarios = List.of();
    }

    private static BigDecimal valorDe(Quadra q, Horario h) {
        long minutos = Duration.between(h.getHoraInicio(), h.getHoraFim()).toMinutes();
        return q.getValorHora().multiply(BigDecimal.valueOf(minutos)).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /** "Beach Tênis" -> "beach tenis" (sem acento, minúsculo). null/vazio -> null. */
    static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) return null;
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    private static boolean contemAlgum(String buscado, String... campos) {
        for (String campo : campos) {
            String n = normalizar(campo);
            if (n != null && n.contains(buscado)) return true;
        }
        return false;
    }
}
