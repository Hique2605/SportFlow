package com.example.SportFlow;

import com.example.SportFlow.Entity.Agendamento;
import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Enum.PagamentoStatus;
import com.example.SportFlow.Enum.Perfil;
import com.example.SportFlow.Enum.StatusAgendamento;
import com.example.SportFlow.Repository.AgendamentoRepository;
import com.example.SportFlow.Repository.PagamentoRepository;
import com.example.SportFlow.Repository.UsuarioRepository;
import com.example.SportFlow.Service.AgendamentoService;
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

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Um teste para cada regra corrigida na revisão de 27/11:
 *  1. valor do pagamento = valor da reserva
 *  2. avaliação só depois do jogo (coberto em CrudIntegrationTests)
 *  3. mudanças de status seguem o ciclo de vida
 *  4. encerramento automático das reservas que já passaram
 *  5. não reserva horário de hoje que já começou
 *  6. quadra ou estabelecimento inativo não aceita reserva
 *  7. reserva dupla simultânea (ver ConcorrenciaAgendamentoTests)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegrasDeNegocioTests {

    private static final String SENHA = "SenhaForte123";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private PagamentoRepository pagamentoRepository;
    @Autowired private AgendamentoService agendamentoService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private Clock relogio;

    private String tokenAdmin;
    private String tokenCliente;
    private String estabelecimentoId;
    private String quadraId;
    private String esporteId;
    private String horarioSegunda;   // segunda-feira, 08:00 às 10:00, quadra de R$ 120/h
    private LocalDate proximaSegunda;

    @BeforeEach
    void preparar() throws Exception {
        Usuario admin = new Usuario("Admin Regras", "admin@regras.test", passwordEncoder.encode(SENHA), null);
        admin.setPerfil(Perfil.ADMIN);
        usuarioRepository.saveAndFlush(admin);
        tokenAdmin = login("admin@regras.test");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Cliente Regras", "email": "cliente@regras.test", "password": "%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isCreated());
        tokenCliente = login("cliente@regras.test");

        esporteId = idDe(comAdmin(post("/api/esportes"), """
                {"nome": "Padel"}
                """));
        estabelecimentoId = idDe(comAdmin(post("/api/estabelecimentos"), """
                {"nome": "Arena Regras", "email": "arena@regras.test"}
                """));
        quadraId = idDe(comAdmin(post("/api/quadras"), """
                {"estabelecimentoId": "%s", "nome": "Padel 1", "valorHora": 120.00, "esporteIds": ["%s"]}
                """.formatted(estabelecimentoId, esporteId)));
        horarioSegunda = criarHorario(1, "08:00:00", "10:00:00");
        proximaSegunda = LocalDate.now(relogio).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    // ------------------------------------------------------------- 1. valor do pagamento

    @Test
    void pagamentoDeveTerOValorDaReserva() throws Exception {
        String reserva = reservar(horarioSegunda, proximaSegunda, "08:00", "09:30"); // 1h30 x 120 = 180

        // valor menor (antes era aceito) -> 400
        pagar(reserva, "\"valor\": 0.01,").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O valor do pagamento deve ser igual ao valor da reserva: R$ 180,00"));

        // sem informar o valor -> usa o valor da reserva
        String pagamento = JsonPath.read(pagar(reserva, "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valor").value(180.0))
                .andReturn().getResponse().getContentAsString(), "$.id");

        // ADMIN também não consegue mudar o valor para outro diferente
        mockMvc.perform(com(put("/pagamentos/{id}", pagamento), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor": 50.00, "forma": "Dinheiro"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------- 3. ciclo de vida do status

    @Test
    void statusDeveSeguirOCicloDeVida() throws Exception {
        String reserva = reservar(horarioSegunda, proximaSegunda, "08:00", "09:00");

        mudarStatus(reserva, "PENDENTE").andExpect(status().isConflict());     // já está
        mudarStatus(reserva, "CONCLUIDO").andExpect(status().isConflict());    // pulou etapa
        mudarStatus(reserva, "CONFIRMADO").andExpect(status().isOk());
        mudarStatus(reserva, "CONCLUIDO").andExpect(status().isConflict())     // jogo ainda não aconteceu
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("A reserva só pode ser concluída depois")));

        moverParaOPassado(reserva);
        mudarStatus(reserva, "CONCLUIDO").andExpect(status().isOk());
        mudarStatus(reserva, "CANCELADO").andExpect(status().isConflict());    // CONCLUIDO é final

        // Cancelada não volta a ficar confirmada (antes voltava, sem checar conflito)
        String outra = reservar(horarioSegunda, proximaSegunda, "09:00", "10:00");
        mockMvc.perform(com(patch("/agendamentos/{id}/cancelar", outra), tokenCliente)).andExpect(status().isNoContent());
        mockMvc.perform(com(patch("/agendamentos/{id}/cancelar", outra), tokenCliente)).andExpect(status().isConflict());
        mudarStatus(outra, "CONFIRMADO").andExpect(status().isConflict());
    }

    @Test
    void cancelarPeloStatusTambemCancelaOPagamentoPendente() throws Exception {
        String reserva = reservar(horarioSegunda, proximaSegunda, "08:00", "09:00");
        pagar(reserva, "").andExpect(status().isCreated());

        mudarStatus(reserva, "CANCELADO").andExpect(status().isOk());

        assertEquals(PagamentoStatus.CANCELADO,
                pagamentoRepository.findByAgendamentoId(UUID.fromString(reserva)).orElseThrow().getStatus());
    }

    // ------------------------------------------------------------- 4. encerramento automático

    @Test
    void encerramentoAutomaticoConcluiConfirmadasECancelaPendentes() throws Exception {
        String confirmada = reservar(horarioSegunda, proximaSegunda, "08:00", "09:00");
        mudarStatus(confirmada, "CONFIRMADO").andExpect(status().isOk());

        String pendente = reservar(horarioSegunda, proximaSegunda, "09:00", "10:00");
        pagar(pendente, "").andExpect(status().isCreated());

        String futura = reservar(horarioSegunda, proximaSegunda.plusWeeks(1), "08:00", "09:00");

        moverParaOPassado(confirmada);
        moverParaOPassado(pendente);

        var resultado = agendamentoService.encerrarAgendamentosPassados();

        assertEquals(1, resultado.concluidos());
        assertEquals(1, resultado.cancelados());
        assertEquals(StatusAgendamento.CONCLUIDO, statusDe(confirmada));
        assertEquals(StatusAgendamento.CANCELADO, statusDe(pendente));
        assertEquals(PagamentoStatus.CANCELADO,
                pagamentoRepository.findByAgendamentoId(UUID.fromString(pendente)).orElseThrow().getStatus());
        assertEquals(StatusAgendamento.PENDENTE, statusDe(futura)); // a futura não é tocada
    }

    // ------------------------------------------------------------- 5. hoje, mas já passou

    @Test
    void naoReservaHorarioDeHojeQueJaComecou() throws Exception {
        LocalDateTime agora = LocalDateTime.now(relogio);
        assumeTrue(agora.getHour() >= 1, "precisa rodar depois da 01:00 para existir um horário que já passou hoje");

        int hoje = agora.getDayOfWeek().getValue() % 7;
        String horarioHoje = criarHorario(hoje, "00:00:00", "00:30:00");

        agendar(horarioHoje, agora.toLocalDate(), "00:00", "00:30")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Não é possível reservar um horário que já começou ou já passou"));
    }

    // ------------------------------------------------------------- 6. inativos

    @Test
    void quadraOuEstabelecimentoInativoNaoAceitaReserva() throws Exception {
        // quadra inativa
        comAdmin(put("/api/quadras/" + quadraId), """
                {"estabelecimentoId": "%s", "nome": "Padel 1", "valorHora": 120.00, "esporteIds": ["%s"], "ativo": false}
                """.formatted(estabelecimentoId, esporteId)).andExpect(status().isOk());
        agendar(horarioSegunda, proximaSegunda, "08:00", "09:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A quadra \"Padel 1\" está inativa e não aceita reservas"));

        // quadra ativa de novo, estabelecimento inativo
        comAdmin(put("/api/quadras/" + quadraId), """
                {"estabelecimentoId": "%s", "nome": "Padel 1", "valorHora": 120.00, "esporteIds": ["%s"], "ativo": true}
                """.formatted(estabelecimentoId, esporteId)).andExpect(status().isOk());
        comAdmin(put("/api/estabelecimentos/" + estabelecimentoId), """
                {"nome": "Arena Regras", "email": "arena@regras.test", "ativo": false}
                """).andExpect(status().isOk());
        agendar(horarioSegunda, proximaSegunda, "08:00", "09:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Este estabelecimento está inativo e não aceita reservas"));
    }

    // ------------------------------------------------------------- auxiliares

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

    private ResultActions comAdmin(MockHttpServletRequestBuilder r, String json) throws Exception {
        return mockMvc.perform(com(r, tokenAdmin).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String idDe(ResultActions r) throws Exception {
        return JsonPath.read(r.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String criarHorario(int diaSemana, String inicio, String fim) throws Exception {
        return idDe(comAdmin(post("/api/horarios"), """
                {"quadraId": "%s", "esporteId": "%s", "diaSemana": %d, "horaInicio": "%s", "horaFim": "%s"}
                """.formatted(quadraId, esporteId, diaSemana, inicio, fim)));
    }

    private ResultActions agendar(String horarioId, LocalDate data, String inicio, String fim) throws Exception {
        return mockMvc.perform(com(post("/agendamentos"), tokenCliente).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"horarioId": "%s", "dataAgendamento": "%s", "horaInicio": "%s", "horaFim": "%s"}
                        """.formatted(horarioId, data, inicio, fim)));
    }

    private String reservar(String horarioId, LocalDate data, String inicio, String fim) throws Exception {
        return idDe(agendar(horarioId, data, inicio, fim));
    }

    private ResultActions pagar(String agendamentoId, String campoValor) throws Exception {
        return mockMvc.perform(com(post("/pagamentos"), tokenCliente).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"agendamentoId": "%s", %s "forma": "PIX"}
                        """.formatted(agendamentoId, campoValor)));
    }

    private ResultActions mudarStatus(String agendamentoId, String novo) throws Exception {
        return mockMvc.perform(com(patch("/agendamentos/{id}/status", agendamentoId), tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "%s"}
                        """.formatted(novo)));
    }

    /** Simula que o jogo já aconteceu: joga a data para a semana passada. */
    private void moverParaOPassado(String agendamentoId) {
        Agendamento a = agendamentoRepository.findById(UUID.fromString(agendamentoId)).orElseThrow();
        a.setDataAgendamento(LocalDate.now(relogio).minusDays(7));
        agendamentoRepository.saveAndFlush(a);
    }

    private StatusAgendamento statusDe(String agendamentoId) {
        return agendamentoRepository.findById(UUID.fromString(agendamentoId)).orElseThrow().getStatus();
    }
}
