package com.vr.miniautorizador.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ObservabilidadeApiTest {

    private static final String AUTORIZACAO = "Basic dXNlcm5hbWU6cGFzc3dvcmQ=";

    @Autowired
    private MockMvc requisicoes;

    @Test
    void deveExporLivenessEReadinessSemDetalhesInternos() throws Exception {
        requisicoes.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        requisicoes.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(content().string(containsString("\"status\":\"UP\"")));
    }

    @Test
    void deveGerarIdDeCorrelacaoUnicoParaCadaRequisicao() throws Exception {
        var primeira = requisicoes.perform(get("/actuator/health/liveness")
                .header("X-Correlation-ID", "valor-fornecido-pelo-cliente"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andReturn().getResponse().getHeader("X-Correlation-ID");
        var segunda = requisicoes.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andReturn().getResponse().getHeader("X-Correlation-ID");

        assertThat(UUID.fromString(primeira)).isNotNull();
        assertThat(primeira).isNotEqualTo("valor-fornecido-pelo-cliente").isNotEqualTo(segunda);
    }

    @Test
    void deveProtegerEndpointsDeMetricasComAutenticacao() throws Exception {
        requisicoes.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());

        requisicoes.perform(get("/actuator/metrics").header("Authorization", AUTORIZACAO))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("miniautorizador.transacoes")));

        requisicoes.perform(get("/actuator/prometheus").header("Authorization", AUTORIZACAO))
                .andExpect(status().isOk());
    }

    @Test
    void deveRegistrarResultadoDaAutorizacaoNasMetricas() throws Exception {
        requisicoes.perform(post("/cartoes")
                .header("Authorization", AUTORIZACAO)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"numeroCartao\":\"8888999900001111\",\"senha\":\"1234\"}"))
                .andExpect(status().isCreated());

        requisicoes.perform(post("/transacoes")
                .header("Authorization", AUTORIZACAO)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"numeroCartao\":\"8888999900001111\",\"senhaCartao\":\"1234\",\"valor\":1.00}"))
                .andExpect(status().isCreated());

        requisicoes.perform(get("/actuator/prometheus").header("Authorization", AUTORIZACAO))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("miniautorizador_transacoes_total")))
                .andExpect(content().string(containsString("resultado=\"OK\"")));
    }
}
