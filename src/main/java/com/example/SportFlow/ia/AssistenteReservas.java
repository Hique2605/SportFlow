package com.example.SportFlow.ia;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.UUID;

/**
 * Interface do assistente (como a CatalogoAIAssistant do PDF).
 *
 * Não tem implementação escrita à mão: o LangChain4j (AiServices) cria uma em tempo de
 * execução. A cada chamada ele monta o prompt (instruções do arquivo .st + histórico da
 * conversa do usuário + mensagem nova), envia ao Gemini e, se o Gemini pedir, executa os
 * métodos @Tool de FerramentasReserva e devolve o resultado para ele, até ter a resposta final.
 */
public interface AssistenteReservas {

    @SystemMessage(fromResource = "/prompts/assistente-reservas.st")
    String conversar(@MemoryId UUID usuarioId,
                     @UserMessage String mensagem,
                     @V("nomeUsuario") String nomeUsuario,
                     @V("hoje") String hoje);
}
