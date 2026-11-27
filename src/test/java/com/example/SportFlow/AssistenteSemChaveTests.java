package com.example.SportFlow;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sem GEMINI_API_KEY: o sistema sobe normalmente e o assistente responde 503 com uma mensagem clara. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AssistenteSemChaveTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void semChaveOAssistenteFicaIndisponivel() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Sem Chave", "email": "semchave@ia.test", "password": "SenhaForte123"}
                                """))
                .andExpect(status().isCreated());
        String token = JsonPath.read(mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "semchave@ia.test", "password": "SenhaForte123"}
                                """))
                .andReturn().getResponse().getContentAsString(), "$.token");

        mockMvc.perform(get("/api/ia/assistente/status").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponivel").value(false));

        mockMvc.perform(post("/api/ia/assistente").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mensagem\": \"oi\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("GEMINI_API_KEY")));
    }
}
