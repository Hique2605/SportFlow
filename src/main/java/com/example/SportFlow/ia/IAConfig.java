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

    @Bean
    @ConditionalOnExpression("!'${sportflow.ia.api-key:}'.isBlank()")
    public ChatModel chatModel(
            @Value("${sportflow.ia.api-key}") String apiKey,
            @Value("${sportflow.ia.modelo}") String modelo,
            @Value("${sportflow.ia.temperatura:0.2}") Double temperatura,
            @Value("${sportflow.ia.timeout-segundos:60}") long timeoutSegundos,
            @Value("${sportflow.ia.log-requisicoes:false}") boolean logRequisicoes) {

        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelo)
                .temperature(temperatura)       // baixa = respostas mais previsíveis
                .timeout(Duration.ofSeconds(timeoutSegundos))
                .maxRetries(1)
                // Gemini 3+: quando o modelo chama uma ferramenta, ele manda uma "assinatura de raciocínio"
                // que precisa voltar na requisição seguinte (senão a API responde 400).
                .returnThinking(true)
                .sendThinking(true)
                .logRequestsAndResponses(logRequisicoes)
                .build();
    }
}
