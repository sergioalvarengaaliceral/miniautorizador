package com.vr.miniautorizador.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/miniautorizador?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.datasource.username=root",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.hibernate.ddl-auto=update"
})
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "mysql.integration", matches = "true")
class TransacaoMySqlIT {

    private static final String AUTORIZACAO = "Basic dXNlcm5hbWU6cGFzc3dvcmQ=";

    @Autowired
    private MockMvc requisicoes;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveSerializarDebitosConcorrentesUsandoOBloqueioDoMySql() throws Exception {
        
    	String numeroCartao = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000_000_000L, 9_999_999_999_999_999L));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicioSimultaneo = new CountDownLatch(1);

        try {
        	
            criarCartao(numeroCartao).andExpect(status().isCreated());

            var transacao = (java.util.concurrent.Callable<Integer>) () -> {
                if (!inicioSimultaneo.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("As transações não foram liberadas para iniciar.");
                }
                return realizarTransacao(numeroCartao).andReturn().getResponse().getStatus();
            };

            Future<Integer> primeira = executor.submit(transacao);
            Future<Integer> segunda = executor.submit(transacao);
            inicioSimultaneo.countDown();

            List<Integer> resultados = List.of(primeira.get(30, TimeUnit.SECONDS), segunda.get(30, TimeUnit.SECONDS));
            assertThat(resultados).containsExactlyInAnyOrder(201, 422);

            requisicoes
            	.perform(get("/cartoes/" + numeroCartao)
            	.header("Authorization", AUTORIZACAO))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("0.00"));
        
        } finally {
            executor.shutdownNow();
            jdbcTemplate.update("delete from cartoes where numero_cartao = ?", numeroCartao);
        }
    }

    private ResultActions criarCartao(String numeroCartao) throws Exception {
        return requisicoes.perform(post("/cartoes")
                .header("Authorization", AUTORIZACAO)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"numeroCartao\":\"" + numeroCartao + "\",\"senha\":\"1234\"}"));
    }

    private ResultActions realizarTransacao(String numeroCartao) throws Exception {
        return requisicoes.perform(post("/transacoes")
                .header("Authorization", AUTORIZACAO)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"numeroCartao\":\"" + numeroCartao + "\",\"senhaCartao\":\"1234\",\"valor\":500.00}"));
    }
    
}