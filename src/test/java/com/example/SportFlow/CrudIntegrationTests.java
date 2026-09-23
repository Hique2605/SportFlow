package com.example.SportFlow;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CrudIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveExecutarCrudDeEsporteQuadraEHorario() throws Exception {
        UUID estabelecimentoId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO estabelecimento (id, nome, email) VALUES (?, ?, ?)",
                estabelecimentoId,
                "SportFlow Arena",
                "arena@sportflow.test"
        );

        String esporteJson = mockMvc.perform(post("/api/esportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Futebol",
                                  "descricao": "Futebol de campo"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Futebol"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String esporteId = JsonPath.read(esporteJson, "$.id");

        String quadraJson = mockMvc.perform(post("/api/quadras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estabelecimentoId": "%s",
                                  "nome": "Quadra principal",
                                  "descricao": "Quadra coberta",
                                  "valorHora": 120.00,
                                  "ativo": true
                                }
                                """.formatted(estabelecimentoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Quadra principal"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String quadraId = JsonPath.read(quadraJson, "$.id");

        String horarioJson = mockMvc.perform(post("/api/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quadraId": "%s",
                                  "esporteId": "%s",
                                  "diaSemana": 1,
                                  "horaInicio": "08:00:00",
                                  "horaFim": "09:00:00",
                                  "ativo": true
                                }
                                """.formatted(quadraId, esporteId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.diaSemana").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String horarioId = JsonPath.read(horarioJson, "$.id");

        mockMvc.perform(get("/api/esportes/{id}", esporteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Futebol"));

        mockMvc.perform(get("/api/quadras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(quadraId));

        mockMvc.perform(get("/api/horarios/{id}", horarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quadraId").value(quadraId));

        mockMvc.perform(put("/api/esportes/{id}", esporteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Futsal",
                                  "descricao": "Futebol de salão"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Futsal"));

        mockMvc.perform(put("/api/quadras/{id}", quadraId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estabelecimentoId": "%s",
                                  "nome": "Quadra atualizada",
                                  "valorHora": 150.00,
                                  "ativo": true
                                }
                                """.formatted(estabelecimentoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Quadra atualizada"));

        mockMvc.perform(put("/api/horarios/{id}", horarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quadraId": "%s",
                                  "esporteId": "%s",
                                  "diaSemana": 2,
                                  "horaInicio": "10:00:00",
                                  "horaFim": "11:00:00",
                                  "ativo": true
                                }
                                """.formatted(quadraId, esporteId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diaSemana").value(2));

        mockMvc.perform(delete("/api/horarios/{id}", horarioId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/quadras/{id}", quadraId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/esportes/{id}", esporteId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/horarios/{id}", horarioId))
                .andExpect(status().isNotFound());
    }
}
