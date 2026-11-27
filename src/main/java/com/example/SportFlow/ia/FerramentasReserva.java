package com.example.SportFlow.ia;

import com.example.SportFlow.Service.DisponibilidadeService;
import com.example.SportFlow.Service.DisponibilidadeService.OpcaoLivre;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ferramentas que o Gemini pode chamar durante a conversa (function calling).
 *
 * O LangChain4j lê as anotações @Tool/@P e descreve cada método para o modelo
 * (nome, para que serve, parâmetros). Quando o modelo decide usar uma, o LangChain4j
 * chama o método Java e devolve o texto de retorno para o modelo.
 *
 * Os dados vêm SEMPRE do banco (DisponibilidadeService); a IA só interpreta.
 * Uma instância por pergunta, já sabendo quem é o usuário.
 */
public class FerramentasReserva {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DisponibilidadeService disponibilidade;
    private final MemoriaAssistente memoria;
    private final UUID usuarioId;

    public FerramentasReserva(DisponibilidadeService disponibilidade, MemoriaAssistente memoria, UUID usuarioId) {
        this.disponibilidade = disponibilidade;
        this.memoria = memoria;
        this.usuarioId = usuarioId;
    }

    @Tool("Lista os esportes que existem no SportFlow. Use para descobrir o nome exato de um esporte.")
    public String listarEsportes() {
        List<String> nomes = disponibilidade.nomesDosEsportes();
        return nomes.isEmpty() ? "Nenhum esporte cadastrado." : String.join(", ", nomes);
    }

    @Tool("Lista os estabelecimentos ativos com bairro, cidade, esportes, faixa de preço por hora e nota média das avaliações.")
    public String listarEstabelecimentos() {
        var lista = disponibilidade.resumoDosEstabelecimentos();
        if (lista.isEmpty()) return "Nenhum estabelecimento ativo.";
        return lista.stream().map(e -> "- %s (%s) | esportes: %s | R$ %s a R$ %s por hora | %s".formatted(
                        e.nome(), local(e.bairro(), e.cidade()), String.join(", ", e.esportes()),
                        dinheiro(e.menorValorHora()), dinheiro(e.maiorValorHora()),
                        e.notaMedia() == null ? "sem avaliações" : "nota " + e.notaMedia() + " (" + e.avaliacoes() + " avaliações)"))
                .collect(Collectors.joining("\n"));
    }

    @Tool("""
            Busca horários LIVRES para reservar. Retorna opções com código (OP1, OP2...).
            Todos os parâmetros são opcionais. Sem datas, busca nos próximos 7 dias (máximo 14 dias).""")
    public String buscarHorariosLivres(
            @P(value = "Nome (ou parte do nome) do esporte, ex.: Futebol, Beach Tennis", required = false) String esporte,
            @P(value = "Primeira data da busca no formato AAAA-MM-DD", required = false) String dataInicial,
            @P(value = "Última data da busca no formato AAAA-MM-DD", required = false) String dataFinal,
            @P(value = "Horário mínimo de início, formato HH:mm (ex.: 18:00)", required = false) String horaMinima,
            @P(value = "Horário máximo de início, formato HH:mm (ex.: 22:00)", required = false) String horaMaxima,
            @P(value = "Valor máximo que a pessoa quer pagar pela reserva, em reais", required = false) Double valorMaximo,
            @P(value = "Nome do estabelecimento, bairro ou cidade", required = false) String local) {

        DisponibilidadeService.Filtro filtro;
        try {
            filtro = new DisponibilidadeService.Filtro(esporte, data(dataInicial), data(dataFinal),
                    hora(horaMinima), hora(horaMaxima),
                    valorMaximo == null ? null : BigDecimal.valueOf(valorMaximo), local, 8);
        } catch (DateTimeParseException e) {
            return "Parâmetro inválido: use datas AAAA-MM-DD e horas HH:mm.";
        }

        List<OpcaoLivre> opcoes = disponibilidade.buscar(filtro);
        if (opcoes.isEmpty()) {
            return "Nenhum horário livre com esses filtros. Sugira mudar o dia, o horário ou o esporte.";
        }

        return opcoes.stream().map(o -> "%s | %s (%s) | %s | %s | %s %s | %s às %s | R$ %s | %s".formatted(
                        memoria.registrarOpcao(usuarioId, o),
                        o.estabelecimentoNome(), local(o.bairro(), o.cidade()), o.quadraNome(), o.esporteNome(),
                        o.data().getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR), o.data().format(DATA),
                        o.horaInicio(), o.horaFim(), dinheiro(o.valor()),
                        o.notaMedia() == null ? "sem avaliações" : "nota " + o.notaMedia()))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Lista as próximas reservas (pendentes ou confirmadas) do usuário que está conversando.")
    public String minhasReservas() {
        var reservas = disponibilidade.proximasReservas(usuarioId);
        if (reservas.isEmpty()) return "O usuário não tem reservas futuras.";
        return reservas.stream().map(r -> "- %s %s, %s às %s | %s | %s | %s | %s | R$ %s".formatted(
                        r.data().getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR), r.data().format(DATA),
                        r.horaInicio(), r.horaFim(), r.estabelecimentoNome(), r.quadraNome(), r.esporteNome(),
                        r.status(), dinheiro(r.valor())))
                .collect(Collectors.joining("\n"));
    }

    // ------------------------------------------------------------------ auxiliares

    private static LocalDate data(String texto) {
        return texto == null || texto.isBlank() ? null : LocalDate.parse(texto.trim());
    }

    private static LocalTime hora(String texto) {
        return texto == null || texto.isBlank() ? null : LocalTime.parse(texto.trim().length() == 4 ? "0" + texto.trim() : texto.trim());
    }

    private static String dinheiro(BigDecimal v) {
        return v == null ? "-" : v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String local(String bairro, String cidade) {
        String l = java.util.stream.Stream.of(bairro, cidade).filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
        return l.isEmpty() ? "local não informado" : l;
    }
}
