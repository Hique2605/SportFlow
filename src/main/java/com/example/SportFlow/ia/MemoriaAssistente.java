package com.example.SportFlow.ia;

import com.example.SportFlow.Service.DisponibilidadeService.OpcaoLivre;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * O que o assistente lembra de cada usuário (em memória; some ao reiniciar o backend):
 *
 *  - histórico da conversa (últimas 20 mensagens) -> a IA entende "e no domingo?" depois de "sábado à tarde"
 *  - opções de horário que as ferramentas já encontraram, com códigos OP1, OP2...
 *    A IA só cita o código; quem transforma o código em horário de verdade é o backend.
 *    Assim a IA não consegue "inventar" um horário com id ou preço errado.
 *  - limite de mensagens por minuto (protege a cota gratuita do Gemini)
 */
@Component
public class MemoriaAssistente implements ChatMemoryProvider {

    private static final int MENSAGENS_NO_HISTORICO = 20;
    private static final int LIMITE_POR_MINUTO = 10;

    private final Map<Object, ChatMemory> conversas = new ConcurrentHashMap<>();
    private final Map<UUID, Sessao> sessoes = new ConcurrentHashMap<>();

    /** Chamado pelo LangChain4j: devolve (ou cria) o histórico daquele usuário. */
    @Override
    public ChatMemory get(Object usuarioId) {
        return conversas.computeIfAbsent(usuarioId, id -> MessageWindowChatMemory.builder()
                .id(id)
                .maxMessages(MENSAGENS_NO_HISTORICO)
                .build());
    }

    /** Guarda uma opção encontrada e devolve o código que a IA vai usar (OP1, OP2...). */
    public String registrarOpcao(UUID usuarioId, OpcaoLivre opcao) {
        Sessao s = sessao(usuarioId);
        // mesma opção encontrada de novo -> mesmo código
        for (Map.Entry<String, OpcaoLivre> e : s.opcoes.entrySet()) {
            if (e.getValue().horarioId().equals(opcao.horarioId()) && e.getValue().data().equals(opcao.data())) {
                return e.getKey();
            }
        }
        String codigo = "OP" + s.contador.incrementAndGet();
        s.opcoes.put(codigo, opcao);
        return codigo;
    }

    public OpcaoLivre opcao(UUID usuarioId, String codigo) {
        return sessao(usuarioId).opcoes.get(codigo.toUpperCase());
    }

    /** true se ainda pode mandar mensagem; false se passou do limite do último minuto. */
    public boolean registrarUso(UUID usuarioId) {
        Deque<Instant> usos = sessao(usuarioId).usos;
        synchronized (usos) {
            Instant agora = Instant.now();
            while (!usos.isEmpty() && usos.peekFirst().isBefore(agora.minus(Duration.ofMinutes(1)))) {
                usos.pollFirst();
            }
            if (usos.size() >= LIMITE_POR_MINUTO) return false;
            usos.addLast(agora);
            return true;
        }
    }

    /** "Nova conversa": apaga histórico e opções do usuário. */
    public void limpar(UUID usuarioId) {
        ChatMemory memoria = conversas.remove(usuarioId);
        if (memoria != null) memoria.clear();
        Sessao s = sessoes.get(usuarioId);
        if (s != null) s.opcoes.clear();
    }

    private Sessao sessao(UUID usuarioId) {
        return sessoes.computeIfAbsent(usuarioId, id -> new Sessao());
    }

    private static class Sessao {
        final Map<String, OpcaoLivre> opcoes = new LinkedHashMap<>();
        final AtomicInteger contador = new AtomicInteger();
        final Deque<Instant> usos = new ArrayDeque<>();
    }
}
