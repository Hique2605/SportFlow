package com.example.SportFlow;

import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Enum.Perfil;
import com.example.SportFlow.Repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração passando por TODAS as camadas
 * (Security/JWT -> Controller -> Service -> Repository -> banco H2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CrudIntegrationTests {

    private static final String SENHA = "SenhaForte123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenAdmin;
    private String tokenCliente;
    private String clienteId;

    @BeforeEach
    void prepararUsuarios() throws Exception {
        Usuario admin = new Usuario("Admin Teste", "admin@teste.com", passwordEncoder.encode(SENHA), null);
        admin.setPerfil(Perfil.ADMIN);
        usuarioRepository.saveAndFlush(admin);
        tokenAdmin = login("admin@teste.com", SENHA);

        String registro = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Cliente Teste", "email": "cliente@teste.com", "password": "%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andReturn().getResponse().getContentAsString();
        clienteId = JsonPath.read(registro, "$.id");
        tokenCliente = login("cliente@teste.com", SENHA);
    }

    // ------------------------------------------------------------------ segurança

    @Test
    void deveAplicarAutenticacaoEAutorizacao() throws Exception {
        // sem token -> 401
        mockMvc.perform(get("/api/esportes")).andExpect(status().isUnauthorized());

        // token inválido -> 401
        mockMvc.perform(get("/api/esportes").header(HttpHeaders.AUTHORIZATION, "Bearer token-falso"))
                .andExpect(status().isUnauthorized());

        // senha errada -> 401
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "cliente@teste.com", "password": "errada123"}
                                """))
                .andExpect(status().isUnauthorized());

        // cliente lê o catálogo, mas não altera -> 403
        mockMvc.perform(com(get("/api/esportes"), tokenCliente)).andExpect(status().isOk());
        mockMvc.perform(com(post("/api/esportes"), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Vôlei"}
                                """))
                .andExpect(status().isForbidden());

        // cliente não lista usuários, mas vê os próprios dados
        mockMvc.perform(com(get("/usuarios"), tokenCliente)).andExpect(status().isForbidden());
        mockMvc.perform(com(get("/usuarios/me"), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente@teste.com"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        // e-mail repetido no cadastro -> 409
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Outro", "email": "cliente@teste.com", "password": "%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isConflict());
    }

    @Test
    void usuarioCriadoPeloAdminDeveConseguirLogar() throws Exception {
        // Antes desta correção a senha era salva sem BCrypt e o login falhava
        mockMvc.perform(com(post("/usuarios"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Novo Usuário", "email": "novo@teste.com", "senha": "%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        login("novo@teste.com", SENHA);
    }

    // ------------------------------------------------------------------ validações

    @Test
    void deveValidarDadosDeEntrada() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "X", "email": "nao-e-email", "password": "123"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(com(post("/api/estabelecimentos"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "", "email": "invalido", "estado": "SCC"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());

        mockMvc.perform(com(post("/api/horarios"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diaSemana": 9}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ CRUD do catálogo

    @Test
    void deveExecutarCrudDoCatalogo() throws Exception {
        String estabelecimentoId = criarEstabelecimento();
        String esporteId = criarEsporte("Futebol");
        String quadraId = criarQuadra(estabelecimentoId, esporteId);
        String horarioId = criarHorario(quadraId, esporteId);

        mockMvc.perform(com(get("/api/esportes/{id}", esporteId), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Futebol"));

        mockMvc.perform(com(put("/api/esportes/{id}", esporteId), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Futsal", "descricao": "Futebol de salão"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Futsal"));

        mockMvc.perform(com(put("/api/quadras/{id}", quadraId), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estabelecimentoId": "%s", "nome": "Quadra atualizada", "valorHora": 150.00, "esporteIds": ["%s"]}
                                """.formatted(estabelecimentoId, esporteId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Quadra atualizada"));

        mockMvc.perform(com(post("/api/funcionarios"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuarioId": "%s", "estabelecimentoId": "%s", "cargo": "Recepcionista", "dataAdmissao": "2026-01-10"}
                                """.formatted(clienteId, estabelecimentoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cargo").value("Recepcionista"));

        mockMvc.perform(com(delete("/api/horarios/{id}", horarioId), tokenAdmin)).andExpect(status().isNoContent());
        mockMvc.perform(com(delete("/api/quadras/{id}", quadraId), tokenAdmin)).andExpect(status().isNoContent());
        mockMvc.perform(com(delete("/api/esportes/{id}", esporteId), tokenAdmin)).andExpect(status().isNoContent());

        mockMvc.perform(com(get("/api/horarios/{id}", horarioId), tokenAdmin)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ fluxo principal

    @Test
    void deveExecutarFluxoAgendamentoPagamentoAvaliacao() throws Exception {
        String esporteId = criarEsporte("Tênis");
        String quadraId = criarQuadra(criarEstabelecimento(), esporteId);
        String horarioId = criarHorario(quadraId, esporteId);
        LocalDate segunda = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        // 1) cliente agenda (valor calculado: 120,00/h x 1h)
        String agendamentoJson = agendar(horarioId, segunda, "08:00", "09:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(clienteId))
                .andExpect(jsonPath("$.valorTotal").value(120.0))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn().getResponse().getContentAsString();
        String agendamentoId = JsonPath.read(agendamentoJson, "$.id");

        // conflito na mesma quadra -> 409
        agendar(horarioId, segunda, "08:30", "09:00").andExpect(status().isConflict());

        // data que não é segunda -> 400
        agendar(horarioId, segunda.plusDays(1), "08:00", "09:00").andExpect(status().isBadRequest());

        // 2) cliente paga
        String pagamentoJson = mockMvc.perform(com(post("/pagamentos"), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agendamentoId": "%s", "valor": 120.00, "forma": "PIX"}
                                """.formatted(agendamentoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn().getResponse().getContentAsString();
        String pagamentoId = JsonPath.read(pagamentoJson, "$.id");

        // cliente não pode confirmar o próprio pagamento -> 403
        mockMvc.perform(com(patch("/pagamentos/{id}/confirmar", pagamentoId), tokenCliente))
                .andExpect(status().isForbidden());

        // 3) admin confirma -> pagamento PAGO e agendamento CONFIRMADO
        mockMvc.perform(com(patch("/pagamentos/{id}/confirmar", pagamentoId), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"));

        mockMvc.perform(com(get("/agendamentos/{id}", agendamentoId), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));

        // 4) cliente avalia
        mockMvc.perform(com(post("/avaliacao"), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agendamentoId": "%s", "nota": 5, "comentario": "Excelente!"}
                                """.formatted(agendamentoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nota").value(5))
                .andExpect(jsonPath("$.agendamentoId").value(agendamentoId));

        // segunda avaliação do mesmo agendamento -> 409 ; nota fora da faixa -> 400
        mockMvc.perform(com(post("/avaliacao"), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agendamentoId": "%s", "nota": 4}
                                """.formatted(agendamentoId)))
                .andExpect(status().isConflict());

        mockMvc.perform(com(post("/avaliacao"), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agendamentoId": "%s", "nota": 6}
                                """.formatted(agendamentoId)))
                .andExpect(status().isBadRequest());

        // 5) outro cliente não enxerga o agendamento -> 403
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Intruso", "email": "intruso@teste.com", "password": "%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isCreated());
        String tokenIntruso = login("intruso@teste.com", SENHA);

        mockMvc.perform(com(get("/agendamentos/{id}", agendamentoId), tokenIntruso))
                .andExpect(status().isForbidden());
        mockMvc.perform(com(get("/agendamentos"), tokenIntruso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------------ gestor e esportes da quadra

    @Test
    void gestorGerenciaSomenteOsProprios_EstabelecimentosEQuadraSoAceitaSeusEsportes() throws Exception {
        String futebol = criarEsporte("Futebol");
        String volei = criarEsporte("Vôlei");
        String meu = criarEstabelecimento("Arena do Gestor", "gestor@arena.test");
        String outro = criarEstabelecimento("Outra Arena", "outra@arena.test");
        String outroAinda = criarEstabelecimento("Terceira Arena", "terceira@arena.test");

        // O cliente vira gestor (funcionário) de DOIS estabelecimentos -> permitido desde a V1.4
        vincularFuncionario(clienteId, meu);
        vincularFuncionario(clienteId, outroAinda);
        // ...mas não duas vezes no mesmo -> 409
        mockMvc.perform(com(post("/api/funcionarios"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuarioId": "%s", "estabelecimentoId": "%s", "cargo": "Gerente", "dataAdmissao": "2026-01-10"}
                                """.formatted(clienteId, meu)))
                .andExpect(status().isConflict());

        // /usuarios/me/estabelecimentos lista o que ele gerencia
        mockMvc.perform(com(get("/usuarios/me/estabelecimentos"), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Edita o próprio estabelecimento (200), o dos outros não (403)
        String dadosEstab = """
                {"nome": "%s", "email": "%s", "estado": "SC", "cep": "88801-000"}
                """;
        mockMvc.perform(com(put("/api/estabelecimentos/{id}", meu), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosEstab.formatted("Arena do Gestor (editada)", "gestor@arena.test")))
                .andExpect(status().isOk());
        mockMvc.perform(com(put("/api/estabelecimentos/{id}", outro), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosEstab.formatted("Invasão", "outra@arena.test")))
                .andExpect(status().isForbidden());

        // Criar estabelecimento e esporte continua sendo só do ADMIN
        mockMvc.perform(com(post("/api/estabelecimentos"), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dadosEstab.formatted("Nova", "nova@arena.test")))
                .andExpect(status().isForbidden());

        // Quadra: sem esportes -> 400 ; no próprio estabelecimento -> 201 ; no dos outros -> 403
        String quadraJson = """
                {"estabelecimentoId": "%s", "nome": "Areia 1", "valorHora": 80.00, "esporteIds": [%s]}
                """;
        mockMvc.perform(com(post("/api/quadras"), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quadraJson.formatted(meu, "")))
                .andExpect(status().isBadRequest());
        String quadra = idDe(mockMvc.perform(com(post("/api/quadras"), tokenCliente)
                .contentType(MediaType.APPLICATION_JSON)
                .content(quadraJson.formatted(meu, "\"" + volei + "\""))));
        mockMvc.perform(com(post("/api/quadras"), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quadraJson.formatted(outro, "\"" + volei + "\"")))
                .andExpect(status().isForbidden());
        mockMvc.perform(com(get("/api/quadras/{id}", quadra), tokenCliente))
                .andExpect(jsonPath("$.esporteIds[0]").value(volei));

        // Horário com esporte que a quadra não aceita (futebol na areia) -> 400 ; com vôlei -> 201
        String horarioJson = """
                {"quadraId": "%s", "esporteId": "%s", "diaSemana": 1, "horaInicio": "18:00:00", "horaFim": "19:00:00"}
                """;
        mockMvc.perform(com(post("/api/horarios"), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(horarioJson.formatted(quadra, futebol)))
                .andExpect(status().isBadRequest());
        String horario = idDe(mockMvc.perform(com(post("/api/horarios"), tokenCliente)
                .contentType(MediaType.APPLICATION_JSON)
                .content(horarioJson.formatted(quadra, volei))));

        // Tirar o vôlei da quadra enquanto há horário de vôlei -> 409
        mockMvc.perform(com(put("/api/quadras/{id}", quadra), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quadraJson.formatted(meu, "\"" + futebol + "\"")))
                .andExpect(status().isConflict());

        // Outro cliente reserva e paga no estabelecimento do gestor
        String tokenJogador = registrarELogar("Jogador", "jogador@teste.com");
        LocalDate segunda = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        String agendamento = idDe(mockMvc.perform(com(post("/agendamentos"), tokenJogador)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"horarioId": "%s", "dataAgendamento": "%s", "horaInicio": "18:00", "horaFim": "19:00"}
                        """.formatted(horario, segunda))));
        String pagamento = idDe(mockMvc.perform(com(post("/pagamentos"), tokenJogador)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"agendamentoId": "%s", "valor": 80.00, "forma": "PIX"}
                        """.formatted(agendamento))));

        // O gestor enxerga a reserva e o pagamento (com o nome do cliente)
        mockMvc.perform(com(get("/agendamentos"), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].usuarioNome".formatted(agendamento)).value("Jogador"));
        mockMvc.perform(com(get("/pagamentos/{id}", pagamento), tokenCliente)).andExpect(status().isOk());

        // O próprio jogador não confirma; o gestor confirma -> reserva CONFIRMADA
        mockMvc.perform(com(patch("/pagamentos/{id}/confirmar", pagamento), tokenJogador))
                .andExpect(status().isForbidden());
        mockMvc.perform(com(patch("/pagamentos/{id}/confirmar", pagamento), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"));
        mockMvc.perform(com(get("/agendamentos/{id}", agendamento), tokenJogador))
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));

        // O gestor muda o status da reserva; um estranho não vê nem muda
        mockMvc.perform(com(patch("/agendamentos/{id}/status", agendamento), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CONCLUIDO"}
                                """))
                .andExpect(status().isOk());
        String tokenEstranho = registrarELogar("Estranho", "estranho@teste.com");
        mockMvc.perform(com(get("/agendamentos/{id}", agendamento), tokenEstranho)).andExpect(status().isForbidden());
        mockMvc.perform(com(patch("/agendamentos/{id}/status", agendamento), tokenEstranho)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CANCELADO"}
                                """))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ auxiliares

    private String login(String email, String senha) throws Exception {
        String resposta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    private MockHttpServletRequestBuilder com(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String criarEstabelecimento() throws Exception {
        return criarEstabelecimento("SportFlow Arena", "arena@sportflow.test");
    }

    private String criarEstabelecimento(String nome, String email) throws Exception {
        return idDe(mockMvc.perform(com(post("/api/estabelecimentos"), tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome": "%s", "email": "%s", "estado": "SC", "cep": "88801-000"}
                        """.formatted(nome, email))));
    }

    private String vincularFuncionario(String usuarioId, String estabelecimentoId) throws Exception {
        return idDe(mockMvc.perform(com(post("/api/funcionarios"), tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"usuarioId": "%s", "estabelecimentoId": "%s", "cargo": "Gerente", "dataAdmissao": "2026-01-10"}
                        """.formatted(usuarioId, estabelecimentoId))));
    }

    private String registrarELogar(String nome, String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "email": "%s", "password": "%s"}
                                """.formatted(nome, email, SENHA)))
                .andExpect(status().isCreated());
        return login(email, SENHA);
    }

    private String criarEsporte(String nome) throws Exception {
        return idDe(mockMvc.perform(com(post("/api/esportes"), tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome": "%s", "descricao": "Teste"}
                        """.formatted(nome))));
    }

    private String criarQuadra(String estabelecimentoId, String esporteId) throws Exception {
        return idDe(mockMvc.perform(com(post("/api/quadras"), tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"estabelecimentoId": "%s", "nome": "Quadra principal", "valorHora": 120.00, "ativo": true, "esporteIds": ["%s"]}
                        """.formatted(estabelecimentoId, esporteId))));
    }

    private String criarHorario(String quadraId, String esporteId) throws Exception {
        return idDe(mockMvc.perform(com(post("/api/horarios"), tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"quadraId": "%s", "esporteId": "%s", "diaSemana": 1, "horaInicio": "08:00:00", "horaFim": "10:00:00"}
                        """.formatted(quadraId, esporteId))));
    }

    private ResultActions agendar(String horarioId, LocalDate data, String inicio, String fim) throws Exception {
        return mockMvc.perform(com(post("/agendamentos"), tokenCliente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"horarioId": "%s", "dataAgendamento": "%s", "horaInicio": "%s", "horaFim": "%s"}
                        """.formatted(horarioId, data, inicio, fim)));
    }

    private String idDe(ResultActions resultado) throws Exception {
        String json = resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }
}
