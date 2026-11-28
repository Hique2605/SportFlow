package com.example.SportFlow;

import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Enum.Perfil;
import com.example.SportFlow.Repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o assistente de IA de ponta a ponta SEM chamar o Gemini de verdade:
 * o ChatModel é trocado por um "Gemini falso" que segue um roteiro.
 *
 *   1ª chamada (mensagem do usuário)  -> pede para executar a ferramenta buscarHorariosLivres
 *   2ª chamada (resultado da ferramenta) -> responde um texto recomendando a 1ª opção + "SUGESTOES: OPx, OP99"
 *
 * Assim conferimos: o LangChain4j chama nosso @Tool, a busca só devolve horários livres,
 * o backend troca o código pelo horário real e ignora código inventado (OP99).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AssistenteIATests.GeminiFalsoConfig.class)
class AssistenteIATests {

    private static final String SENHA = "SenhaForte123";

    /** Guarda tudo o que o "Gemini" recebeu, para o teste conferir. */
    static final List<ChatRequest> RECEBIDOS = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class GeminiFalsoConfig {
        @Bean(name = "chatModel")
        ChatModel geminiFalso() {
            return new ChatModel() {
                @Override
                public ChatResponse doChat(ChatRequest request) {
                    RECEBIDOS.add(request);
                    List<ChatMessage> msgs = request.messages();
                    ChatMessage ultima = msgs.get(msgs.size() - 1);

                    if (ultima instanceof UserMessage u && u.singleText().toLowerCase().contains("padel")) {
                        return resposta(AiMessage.from(List.of(ToolExecutionRequest.builder()
                                .id("1").name("buscarHorariosLivres")
                                .arguments("{\"esporte\": \"padel\"}")
                                .build())));
                    }
                    if (ultima instanceof ToolExecutionResultMessage r) {
                        Matcher m = Pattern.compile("OP\\d+").matcher(r.text());
                        String primeiro = m.find() ? m.group() : "OP0";
                        return resposta(AiMessage.from(
                                "Achei uma boa opção na Arena Teste (" + primeiro + "), segunda às 09:00 por R$ 120,00. "
                                        + "Clique em Reservar se quiser.\nSUGESTOES: " + primeiro + ", OP99"));
                    }
                    return resposta(AiMessage.from("Posso ajudar a encontrar um horário. Qual esporte?"));
                }
            };
        }

        private static ChatResponse resposta(AiMessage m) {
            return ChatResponse.builder().aiMessage(m).build();
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String tokenAdmin;
    private String tokenCliente;
    private String horarioId;
    private LocalDate segunda;

    @BeforeEach
    void preparar() throws Exception {
        RECEBIDOS.clear();
        Usuario admin = new Usuario("Admin IA", "admin@ia.test", passwordEncoder.encode(SENHA), null);
        admin.setPerfil(Perfil.ADMIN);
        usuarioRepository.saveAndFlush(admin);
        tokenAdmin = login("admin@ia.test");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Joana", "email": "joana@ia.test", "password": "%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isCreated());
        tokenCliente = login("joana@ia.test");

        String esporte = criar("/api/esportes", """
                {"nome": "Padel"}
                """);
        String estab = criar("/api/estabelecimentos", """
                {"nome": "Arena Teste", "email": "arena@ia.test", "bairro": "Centro", "cidade": "Criciúma"}
                """);
        String quadra = criar("/api/quadras", """
                {"estabelecimentoId": "%s", "nome": "Padel 1", "valorHora": 120.00, "esporteIds": ["%s"]}
                """.formatted(estab, esporte));
        // Dois horários na segunda: 08h e 09h
        horarioId = criar("/api/horarios", """
                {"quadraId": "%s", "esporteId": "%s", "diaSemana": 1, "horaInicio": "08:00:00", "horaFim": "09:00:00"}
                """.formatted(quadra, esporte));
        criar("/api/horarios", """
                {"quadraId": "%s", "esporteId": "%s", "diaSemana": 1, "horaInicio": "09:00:00", "horaFim": "10:00:00"}
                """.formatted(quadra, esporte));
        segunda = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    @Test
    void statusIndicaQueAIaEstaDisponivel() throws Exception {
        mockMvc.perform(com(get("/api/ia/assistente/status"), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponivel").value(true))
                .andExpect(jsonPath("$.provedor").value("Google Gemini"));
    }

    @Test
    void assistenteSugereSomenteHorarioLivreComDadosReais() throws Exception {
        // A segunda às 08h já está ocupada -> não pode ser sugerida
        mockMvc.perform(com(post("/agendamentos"), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"horarioId": "%s", "dataAgendamento": "%s", "horaInicio": "08:00", "horaFim": "09:00"}
                                """.formatted(horarioId, segunda)))
                .andExpect(status().isCreated());

        String json = conversar("Quero jogar padel na segunda de manhã")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sugestoes.length()").value(1))       // OP99 (inventado) foi ignorado
                .andExpect(jsonPath("$.sugestoes[0].dataAgendamento").value(segunda.toString()))
                .andExpect(jsonPath("$.sugestoes[0].horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$.sugestoes[0].estabelecimentoNome").value("Arena Teste"))
                .andExpect(jsonPath("$.sugestoes[0].valor").value(120.0))
                .andReturn().getResponse().getContentAsString();

        String mensagem = JsonPath.read(json, "$.mensagem");
        assertThat(mensagem).startsWith("Achei uma boa opção na Arena Teste").doesNotContain("SUGESTOES").doesNotContain("OP");

        // O que o "Gemini" recebeu: instruções com o nome do usuário e a data de hoje + as ferramentas
        ChatRequest primeira = RECEBIDOS.get(0);
        SystemMessage sistema = (SystemMessage) primeira.messages().get(0);
        assertThat(sistema.text()).contains("Joana").contains("Assistente de Reservas do SportFlow");
        assertThat(primeira.toolSpecifications()).extracting(ToolSpecification::name)
                .contains("buscarHorariosLivres", "listarEsportes", "listarEstabelecimentos", "minhasReservas");

        // O resultado da ferramenta (dados do banco) não traz o horário ocupado das 08:00
        ToolExecutionResultMessage resultado = (ToolExecutionResultMessage) RECEBIDOS.get(1).messages()
                .get(RECEBIDOS.get(1).messages().size() - 1);
        assertThat(resultado.text()).contains("09:00 às 10:00").doesNotContain("08:00 às 09:00");
    }

    @Test
    void conversaTemMemoriaPorUsuarioEPodeSerReiniciada() throws Exception {
        conversar("oi").andExpect(status().isOk());
        conversar("quero padel").andExpect(status().isOk());

        // na 2ª mensagem o modelo recebe o histórico (sistema + oi + resposta + nova mensagem)
        int antes = RECEBIDOS.get(1).messages().size();
        assertThat(antes).isGreaterThanOrEqualTo(4);

        mockMvc.perform(com(delete("/api/ia/assistente"), tokenCliente)).andExpect(status().isNoContent());
        RECEBIDOS.clear();
        conversar("oi de novo").andExpect(status().isOk());
        assertThat(RECEBIDOS.get(0).messages()).hasSize(2);   // só sistema + mensagem nova
    }

    @Test
    void validaMensagemELimitaQuantidadePorMinuto() throws Exception {
        conversar(" ").andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/ia/assistente").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensagem\": \"oi\"}"))
                .andExpect(status().isUnauthorized());

        for (int i = 0; i < 10; i++) conversar("oi " + i).andExpect(status().isOk());
        conversar("mais uma").andExpect(status().isTooManyRequests());
    }

    // ------------------------------------------------------------------ auxiliares

    private ResultActions conversar(String texto) throws Exception {
        return mockMvc.perform(com(post("/api/ia/assistente"), tokenCliente).contentType(MediaType.APPLICATION_JSON)
                .content("{\"mensagem\": \"" + texto + "\"}"));
    }

    private String login(String email) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
    }

    private MockHttpServletRequestBuilder com(MockHttpServletRequestBuilder r, String token) {
        return r.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String criar(String rota, String json) throws Exception {
        return JsonPath.read(mockMvc.perform(com(post(rota), tokenAdmin).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }
}
