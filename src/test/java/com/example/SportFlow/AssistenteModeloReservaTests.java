package com.example.SportFlow;

import com.jayway.jsonpath.JsonPath;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Se o modelo principal falhar (ex.: 503 "high demand" do Google), o assistente
 * tenta o modelo reserva, sem repetir a mensagem do usuário no histórico.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AssistenteModeloReservaTests.ModelosConfig.class)
class AssistenteModeloReservaTests {

    static final List<ChatRequest> RESERVA_RECEBEU = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class ModelosConfig {
        @Bean(name = "chatModel")
        ChatModel principalSobrecarregado() {
            return new ChatModel() {
                @Override
                public ChatResponse doChat(ChatRequest request) {
                    throw new RuntimeException("503 UNAVAILABLE: This model is currently experiencing high demand.");
                }
            };
        }

        @Bean(name = "chatModelReserva")
        ChatModel reserva() {
            return new ChatModel() {
                @Override
                public ChatResponse doChat(ChatRequest request) {
                    RESERVA_RECEBEU.add(request);
                    return ChatResponse.builder().aiMessage(AiMessage.from("Resposta do modelo reserva.")).build();
                }
            };
        }
    }

    @Autowired private MockMvc mockMvc;

    @Test
    void usaOModeloReservaQuandoOPrincipalFalha() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Reserva", "email": "reserva@ia.test", "password": "SenhaForte123"}
                                """))
                .andExpect(status().isCreated());
        String token = JsonPath.read(mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "reserva@ia.test", "password": "SenhaForte123"}
                                """))
                .andReturn().getResponse().getContentAsString(), "$.token");

        mockMvc.perform(post("/api/ia/assistente").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mensagem\": \"oi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Resposta do modelo reserva."));

        // O histórico que chegou ao reserva tem a mensagem do usuário UMA vez só
        long mensagensDoUsuario = RESERVA_RECEBEU.get(0).messages().stream()
                .filter(m -> m instanceof UserMessage).count();
        assertThat(mensagensDoUsuario).isEqualTo(1);
    }
}
