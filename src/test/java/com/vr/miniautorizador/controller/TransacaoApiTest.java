package com.vr.miniautorizador.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class TransacaoApiTest {

    private static final String AUTORIZACAO = "Basic dXNlcm5hbWU6cGFzc3dvcmQ=";

    @Autowired
    private MockMvc requisicoes;

    @Test
    void deveDebitarAteOValorUltrapassarOSaldo() throws Exception {
        
    	criarCartao("1111222233334444", "1234").andExpect(status().isCreated());

        realizarTransacao("1111222233334444", "1234", "499.90")
            .andExpect(status().isCreated())
            .andExpect(content().string("OK"));

        realizarTransacao("1111222233334444", "1234", "0.11")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().string("SALDO_INSUFICIENTE"));

        requisicoes.perform(get("/cartoes/1111222233334444").header("Authorization", AUTORIZACAO))
            .andExpect(status().isOk())
            .andExpect(content().string("0.10"));
    }

    @Test
    void deveRecusarSenhaInvalida() throws Exception {
        
    	criarCartao("9999000011112222", "1234").andExpect(status().isCreated());

        realizarTransacao("9999000011112222", "0000", "1.00")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().string("SENHA_INVALIDA"));
    }

    @Test
    void deveRecusarTransacaoComCartaoInexistente() throws Exception {
        
    	realizarTransacao("0000000000000000", "1234", "1.00")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().string("CARTAO_INEXISTENTE"));
    }

    @Test
    void deveRecusarFracaoDeCentavoSemAlterarOSaldo() throws Exception {
        String numeroCartao = "2222333344445555";
        criarCartao(numeroCartao, "1234").andExpect(status().isCreated());

        realizarTransacao(numeroCartao, "1234", "0.001")
            .andExpect(status().isBadRequest())
            .andExpect(content().string(""));

        requisicoes.perform(get("/cartoes/" + numeroCartao).header("Authorization", AUTORIZACAO))
            .andExpect(status().isOk())
            .andExpect(content().string("500.00"));
    }

    @Test
    void deveSerializarTransacoesConcorrentesDoMesmoCartao() throws Exception {
        
    	String numeroCartao = "5555666677778888";
        criarCartao(numeroCartao, "1234").andExpect(status().isCreated());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicioSimultaneo = new CountDownLatch(1);
        
        java.util.concurrent.Callable<Integer> transacao = () -> {
            inicioSimultaneo.await();
            return realizarTransacao(numeroCartao, "1234", "500.00").andReturn().getResponse().getStatus();
        };

        try {
        	
            Future<Integer> primeira = executor.submit(transacao);
            Future<Integer> segunda = executor.submit(transacao);
            inicioSimultaneo.countDown();

            List<Integer> statusDasTransacoes = List.of(primeira.get(), segunda.get());
            assertThat(statusDasTransacoes).containsExactlyInAnyOrder(201, 422);
            requisicoes.perform(get("/cartoes/" + numeroCartao).header("Authorization", AUTORIZACAO))
                .andExpect(status().isOk())
                .andExpect(content().string("0.00"));
            
        } finally {
            executor.shutdownNow();
        }
    }

    private ResultActions criarCartao(String numeroCartao, String senha) throws Exception {
        return requisicoes.perform(post("/cartoes")
            .header("Authorization", AUTORIZACAO)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"numeroCartao\":\"" + numeroCartao + "\",\"senha\":\"" + senha + "\"}"));
    }

    private ResultActions realizarTransacao(String numeroCartao, String senha, String valor) throws Exception {
        return requisicoes.perform(post("/transacoes")
            .header("Authorization", AUTORIZACAO)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"numeroCartao\":\"" + numeroCartao + "\",\"senhaCartao\":\"" + senha + "\",\"valor\":" + valor + "}"));
    }
    
}
