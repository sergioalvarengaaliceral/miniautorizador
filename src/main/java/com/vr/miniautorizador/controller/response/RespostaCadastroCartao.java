package com.vr.miniautorizador.controller.response;

import static com.vr.miniautorizador.service.cartao.ResultadoCadastroCartao.CADASTRADO;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.ResponseEntity.status;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.vr.miniautorizador.service.cartao.ResultadoCadastroCartao;

@Component
public class RespostaCadastroCartao {

	public ResponseEntity<CadastroCartaoResponse> criar(ResultadoCadastroCartao resultado, CadastroCartaoResponse response) {
		var status = resultado == CADASTRADO ? CREATED : UNPROCESSABLE_ENTITY;
		return status(status).body(response);
	}

}
