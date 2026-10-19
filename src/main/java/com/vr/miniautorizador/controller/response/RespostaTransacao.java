package com.vr.miniautorizador.controller.response;

import static com.vr.miniautorizador.service.transacao.ResultadoAutorizacao.OK;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.ResponseEntity.status;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.vr.miniautorizador.service.transacao.ResultadoAutorizacao;

@Component
public class RespostaTransacao {

	public ResponseEntity<String> criar(ResultadoAutorizacao resultado) {
		var status = resultado == OK ? CREATED : UNPROCESSABLE_ENTITY;
		return status(status).body(resultado.name());
	}

}
