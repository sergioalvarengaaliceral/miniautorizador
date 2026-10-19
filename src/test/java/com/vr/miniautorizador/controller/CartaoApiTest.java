package com.vr.miniautorizador.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class CartaoApiTest {

    private static final String AUTORIZACAO = "Basic dXNlcm5hbWU6cGFzc3dvcmQ=";

    @Autowired
    private MockMvc requisicoes;

    @Test
    void deveDisponibilizarDocumentacaoOpenApiSemAutenticacao() throws Exception {
        
    	requisicoes
    		.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/cartoes")));

        requisicoes
        	.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
    }

    @Test
    void deveCriarCartaoComSaldoInicialERecusarDuplicidade() throws Exception {
        
    	criarCartao("6549873025634501", "1234")
            .andExpect(status().isCreated())
            .andExpect(content().json("{\"numeroCartao\":\"6549873025634501\",\"senha\":\"1234\"}"));

        requisicoes.perform(get("/cartoes/6549873025634501").header("Authorization", AUTORIZACAO))
            .andExpect(status().isOk())
            .andExpect(content().string("500.00"));

        criarCartao("6549873025634501", "1234")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().json("{\"numeroCartao\":\"6549873025634501\",\"senha\":\"1234\"}"));
    }

    @Test
    void deveRetornar404AoConsultarCartaoInexistente() throws Exception {
        
    	requisicoes
    		.perform(get("/cartoes/0000000000000000")
    		.header("Authorization", AUTORIZACAO))
    		.andExpect(status().isNotFound());
    }

    @Test
    void deveExigirAutenticacaoBasic() throws Exception {
        
    	requisicoes
    		.perform(get("/cartoes/6549873025634501"))
            .andExpect(status().isUnauthorized());
    }

    private ResultActions criarCartao(String numeroCartao, String senha) throws Exception {
        return requisicoes
    			.perform(post("/cartoes")
    			.header("Authorization", AUTORIZACAO)
    			.contentType(MediaType.APPLICATION_JSON)
    			.content("{\"numeroCartao\":\"" + numeroCartao + "\",\"senha\":\"" + senha + "\"}"));
    }
    
}
