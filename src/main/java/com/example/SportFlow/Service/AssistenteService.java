package com.example.SportFlow.Service;

import com.example.SportFlow.Dto.IA.RespostaAssistenteResponse;
import com.example.SportFlow.Dto.IA.StatusAssistenteResponse;
import com.example.SportFlow.Dto.IA.SugestaoReservaResponse;
import com.example.SportFlow.Service.DisponibilidadeService.OpcaoLivre;
import com.example.SportFlow.config.UsuarioLogado;
import com.example.SportFlow.ia.AssistenteReservas;
import com.example.SportFlow.ia.FerramentasReserva;
import com.example.SportFlow.ia.MemoriaAssistente;
import com.example.SportFlow.Repository.UsuarioRepository;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orquestra o assistente de reservas:
 *   1. confere se a IA está configurada e se o usuário não passou do limite
 *   2. monta o AiServices (Gemini + ferramentas + memória da conversa) e envia a mensagem
 *   3. separa da resposta a linha "SUGESTOES: OP1, OP3" e troca os códigos pelos horários reais
 *   4. confere de novo se cada horário continua livre antes de devolver
 */
@Slf4j
@Service
public class AssistenteService {

    private static final Pattern LINHA_SUGESTOES = Pattern.compile("(?im)^\\s*SUGEST(?:O|Õ)ES\\s*:\\s*(.*)$");
    private static final Pattern CODIGO = Pattern.compile("\\bOP\\d+\\b", Pattern.CASE_INSENSITIVE);
    private static final int MAXIMO_SUGESTOES = 3;
    private static final DateTimeFormatter HOJE =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy, HH:mm", Locale.of("pt", "BR"));

    private final ObjectProvider<ChatModel> chatModel;
    private final DisponibilidadeService disponibilidade;
    private final MemoriaAssistente memoria;
    private final UsuarioLogado usuarioLogado;
    private final UsuarioRepository usuarioRepository;
    private final Clock relogio;
    private final String modelo;

    public AssistenteService(ObjectProvider<ChatModel> chatModel, DisponibilidadeService disponibilidade,
                             MemoriaAssistente memoria, UsuarioLogado usuarioLogado,
                             UsuarioRepository usuarioRepository, Clock relogio,
                             @Value("${sportflow.ia.modelo:}") String modelo) {
        this.chatModel = chatModel;
        this.disponibilidade = disponibilidade;
        this.memoria = memoria;
        this.usuarioLogado = usuarioLogado;
        this.usuarioRepository = usuarioRepository;
        this.relogio = relogio;
        this.modelo = modelo;
    }

    public StatusAssistenteResponse status() {
        return new StatusAssistenteResponse(chatModel.getIfAvailable() != null, "Google Gemini", modelo);
    }

    public RespostaAssistenteResponse conversar(String mensagem) {
        ChatModel modeloIA = chatModel.getIfAvailable();
        if (modeloIA == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O assistente de IA não está configurado. Defina a variável de ambiente GEMINI_API_KEY e reinicie o backend.");
        }

        UUID usuarioId = usuarioLogado.id();
        if (!memoria.registrarUso(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas mensagens em pouco tempo. Espere um minuto e tente de novo.");
        }

        String nome = usuarioRepository.findById(usuarioId).map(u -> u.getNome()).orElse("cliente");

        AssistenteReservas assistente = AiServices.builder(AssistenteReservas.class)
                .chatModel(modeloIA)
                .chatMemoryProvider(memoria)
                .tools(new FerramentasReserva(disponibilidade, memoria, usuarioId))
                .build();

        String resposta;
        try {
            resposta = assistente.conversar(usuarioId, mensagem.trim(), nome,
                    LocalDateTime.now(relogio).format(HOJE));
        } catch (RateLimitException e) {
            log.warn("Gemini recusou por limite de uso: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "O limite gratuito da IA foi atingido. Tente de novo em alguns instantes.");
        } catch (RuntimeException e) {
            log.error("Falha ao conversar com o Gemini", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O assistente está indisponível agora. Tente de novo em instantes.");
        }

        return montarResposta(usuarioId, resposta == null ? "" : resposta);
    }

    public void novaConversa() {
        memoria.limpar(usuarioLogado.id());
    }

    /** Separa o texto das sugestões e troca os códigos OPn pelos horários reais. */
    RespostaAssistenteResponse montarResposta(UUID usuarioId, String resposta) {
        Set<String> codigos = new LinkedHashSet<>();

        Matcher linha = LINHA_SUGESTOES.matcher(resposta);
        if (linha.find()) {
            Matcher m = CODIGO.matcher(linha.group(1));
            while (m.find()) codigos.add(m.group().toUpperCase());
        }
        // Se o modelo esqueceu a linha final mas citou códigos no texto, aproveita esses
        Matcher noTexto = CODIGO.matcher(resposta);
        while (noTexto.find()) codigos.add(noTexto.group().toUpperCase());

        String texto = LINHA_SUGESTOES.matcher(resposta).replaceAll("")
                .replaceAll("\\s*\\(?\\b[Oo][Pp]\\d+\\b\\)?:?", "")
                .trim();

        List<SugestaoReservaResponse> sugestoes = new ArrayList<>();
        for (String codigo : codigos) {
            if (sugestoes.size() == MAXIMO_SUGESTOES) break;
            OpcaoLivre opcao = memoria.opcao(usuarioId, codigo);
            if (opcao != null && disponibilidade.continuaLivre(opcao)) {   // código desconhecido ou já ocupado: ignora
                sugestoes.add(SugestaoReservaResponse.from(opcao));
            }
        }
        return new RespostaAssistenteResponse(texto, sugestoes);
    }
}
