package com.example.SportFlow;

import com.example.SportFlow.Entity.Usuario;
import com.example.SportFlow.Enum.Perfil;
import com.example.SportFlow.Repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Item 7: vários clientes tentam reservar O MESMO horário AO MESMO TEMPO.
 * Só um pode conseguir (201); os outros recebem 409.
 *
 * Esta classe NÃO usa @Transactional: cada requisição precisa gravar de verdade
 * (commit) para as outras threads enxergarem. @DirtiesContext descarta o banco
 * H2 no final para não sobrar dado para as outras classes de teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConcorrenciaAgendamentoTests {

    private static final String SENHA = "SenhaForte123";
    private static final int CLIENTES = 6;

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void apenasUmaReservaSimultaneaDoMesmoHorarioDeveSerAceita() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        Usuario admin = new Usuario("Admin Conc", "admin-" + sufixo + "@conc.test", passwordEncoder.encode(SENHA), null);
        admin.setPerfil(Perfil.ADMIN);
        usuarioRepository.saveAndFlush(admin);
        String tokenAdmin = login(admin.getEmail());

        String esporte = criar(tokenAdmin, "/api/esportes", """
                {"nome": "Squash %s"}
                """.formatted(sufixo));
        String estab = criar(tokenAdmin, "/api/estabelecimentos", """
                {"nome": "Arena Conc", "email": "arena-%s@conc.test"}
                """.formatted(sufixo));
        String quadra = criar(tokenAdmin, "/api/quadras", """
                {"estabelecimentoId": "%s", "nome": "Squash 1", "valorHora": 60.00, "esporteIds": ["%s"]}
                """.formatted(estab, esporte));
        String horario = criar(tokenAdmin, "/api/horarios", """
                {"quadraId": "%s", "esporteId": "%s", "diaSemana": 3, "horaInicio": "19:00:00", "horaFim": "20:00:00"}
                """.formatted(quadra, esporte));
        LocalDate quarta = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));

        // Um token por cliente
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < CLIENTES; i++) {
            String email = "cli" + i + "-" + sufixo + "@conc.test";
            mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Cliente %d", "email": "%s", "password": "%s"}
                                    """.formatted(i, email, SENHA)))
                    .andExpect(status().isCreated());
            tokens.add(login(email));
        }

        // Todos disparam juntos
        String corpo = """
                {"horarioId": "%s", "dataAgendamento": "%s", "horaInicio": "19:00", "horaFim": "20:00"}
                """.formatted(horario, quarta);
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(CLIENTES);
        List<Future<Integer>> respostas = new ArrayList<>();
        for (String token : tokens) {
            respostas.add(pool.submit(() -> {
                largada.await();
                return mockMvc.perform(post("/agendamentos")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo))
                        .andReturn().getResponse().getStatus();
            }));
        }
        largada.countDown();

        List<Integer> codigos = new ArrayList<>();
        for (Future<Integer> f : respostas) codigos.add(f.get());
        pool.shutdown();

        assertEquals(1, Collections.frequency(codigos, 201), "códigos recebidos: " + codigos);
        assertEquals(CLIENTES - 1, Collections.frequency(codigos, 409), "códigos recebidos: " + codigos);
    }

    private String login(String email) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
    }

    private String criar(String token, String rota, String json) throws Exception {
        return JsonPath.read(mockMvc.perform(post(rota)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }
}
