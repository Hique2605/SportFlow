package com.example.SportFlow;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin@sportflow.test", roles = "ADMIN")
class CrudIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveExecutarCincoCrudsComRelacionamentos() throws Exception {
        String usuarioId = idDaResposta(mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Gestor da Arena",
                                  "email": "gestor@sportflow.test",
                                  "senha": "SenhaSegura123",
                                  "telefone": "48999999999"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andReturn().getResponse().getContentAsString());

        String estabelecimentoId = idDaResposta(mockMvc.perform(post("/api/estabelecimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gestorId": "%s",
                                  "nome": "SportFlow Arena",
                                  "email": "arena@sportflow.test",
                                  "telefone": "4833334444",
                                  "cidade": "Criciúma",
                                  "estado": "SC",
                                  "cep": "88800-000",
                                  "ativo": true
                                }
                                """.formatted(usuarioId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gestorId").value(usuarioId))
                .andReturn().getResponse().getContentAsString());

        String esporteId = idDaResposta(mockMvc.perform(post("/api/esportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Futebol","descricao":"Futebol de campo","ativo":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        String quadraId = idDaResposta(mockMvc.perform(post("/api/quadras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estabelecimentoId": "%s",
                                  "nome": "Quadra principal",
                                  "descricao": "Quadra coberta",
                                  "tipoPiso": "Sintético",
                                  "coberta": true,
                                  "valorHora": 120.00,
                                  "ativo": true
                                }
                                """.formatted(estabelecimentoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estabelecimentoId").value(estabelecimentoId))
                .andReturn().getResponse().getContentAsString());

        String horarioId = idDaResposta(mockMvc.perform(post("/api/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(horarioJson(quadraId, esporteId, 1, "08:00:00", "09:00:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quadraId").value(quadraId))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(horarioJson(quadraId, esporteId, 1, "08:30:00", "09:30:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "Já existe um horário sobreposto para esta quadra e dia da semana"));

        mockMvc.perform(get("/api/usuarios/{id}", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("gestor@sportflow.test"));
        mockMvc.perform(get("/api/estabelecimentos/{id}", estabelecimentoId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/esportes/{id}", esporteId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/quadras/{id}", quadraId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/horarios/{id}", horarioId)).andExpect(status().isOk());

        mockMvc.perform(put("/api/esportes/{id}", esporteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Futsal","descricao":"Futebol de salão","ativo":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Futsal"));

        mockMvc.perform(delete("/api/horarios/{id}", horarioId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/quadras/{id}", quadraId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/esportes/{id}", esporteId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/estabelecimentos/{id}", estabelecimentoId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/usuarios/{id}", usuarioId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/horarios/{id}", horarioId)).andExpect(status().isNotFound());
    }

    private String idDaResposta(String json) {
        return JsonPath.read(json, "$.id");
    }

    private String horarioJson(String quadraId, String esporteId, int dia, String inicio, String fim) {
        return """
                {
                  "quadraId": "%s",
                  "esporteId": "%s",
                  "diaSemana": %d,
                  "horaInicio": "%s",
                  "horaFim": "%s",
                  "ativo": true
                }
                """.formatted(quadraId, esporteId, dia, inicio, fim);
    }
}
