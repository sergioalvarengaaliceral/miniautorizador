package com.vr.miniautorizador.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vr.miniautorizador.controller.request.TransacaoRequest;
import com.vr.miniautorizador.controller.response.RespostaTransacao;
import com.vr.miniautorizador.service.transacao.AutorizacaoService;
import com.vr.miniautorizador.service.transacao.TransacaoDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

	private final AutorizacaoService autorizacaoService;
	private final RespostaTransacao respostaTransacao;

	public TransacaoController(AutorizacaoService autorizacaoService, RespostaTransacao respostaTransacao) {
		this.autorizacaoService = autorizacaoService;
		this.respostaTransacao = respostaTransacao;
	}

	@PostMapping
	public ResponseEntity<String> autorizar(@Valid @RequestBody TransacaoRequest request) {
		var dto = new TransacaoDTO(request.numeroCartao(), request.senhaCartao(), request.valor());
		var resultado = autorizacaoService.autorizar(dto);
		return respostaTransacao.criar(resultado);
	}

}
