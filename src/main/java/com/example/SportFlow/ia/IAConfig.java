package com.example.SportFlow.ia;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuração da IA (LangChain4j + Google Gemini) — mesma ideia do OpenAiConfig do PDF,
 * trocando o provedor: aqui o ChatModel é o GoogleAiGeminiChatModel.
 *
 * A chave vem da variável de ambiente GEMINI_API_KEY (nunca no código nem no git).
 * Sem chave, este bean não é criado: o resto do sistema funciona normalmente e o
 * assistente responde 503 explicando o que falta.
 */
@Configuration
public class IAConfig {

    /** Modelo principal (sportflow.ia.modelo). */
    @Bean(name = "chatModel")
    @ConditionalOnExpression("!'${sportflow.ia.api-key:}'.isBlank()")
    public ChatModel chatModel(
            @Value("${sportflow.ia.api-key}") String apiKey,
            @Value("${sportflow.ia.modelo}") String modelo,
            @Value("${sportflow.ia.temperatura:0.2}") Double temperatura,
            @Value("${sportflow.ia.timeout-segundos:60}") long timeoutSegundos,
            @Value("${sportflow.ia.log-requisicoes:false}") boolean logRequisicoes) {
        return gemini(apiKey, modelo, temperatura, timeoutSegundos, logRequisicoes);
    }

    /**
     * Modelo reserva (sportflow.ia.modelo-reserva): usado quando o principal falha,
     * por exemplo com 503 "model is currently experiencing high demand" do Google.
     */
    @Bean(name = "chatModelReserva")
    @ConditionalOnExpression("!'${sportflow.ia.api-key:}'.isBlank() and !'${sportflow.ia.modelo-reserva:}'.isBlank()")
    public ChatModel chatModelReserva(
            @Value("${sportflow.ia.api-key}") String apiKey,
            @Value("${sportflow.ia.modelo-reserva}") String modelo,
            @Value("${sportflow.ia.temperatura:0.2}") Double temperatura,
            @Value("${sportflow.ia.timeout-segundos:60}") long timeoutSegundos,
            @Value("${sportflow.ia.log-requisicoes:false}") boolean logRequisicoes) {
        return gemini(apiKey, modelo, temperatura, timeoutSegundos, logRequisicoes);
    }

    private static ChatModel gemini(String apiKey, String modelo, Double temperatura,
                                    long timeoutSegundos, boolean logRequisicoes) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelo)
                .temperature(temperatura)       // baixa = respostas mais previsíveis
                .timeout(Duration.ofSeconds(timeoutSegundos))
                .maxRetries(0)                  // sem esperar: se falhar, o AssistenteService tenta o modelo reserva
                // Gemini 3+: quando o modelo chama uma ferramenta, ele manda uma "assinatura de raciocínio"
                // que precisa voltar na requisição seguinte (senão a API responde 400).
                .returnThinking(true)
                .sendThinking(true)
                .logRequestsAndResponses(logRequisicoes)
                .build();
    }
}
