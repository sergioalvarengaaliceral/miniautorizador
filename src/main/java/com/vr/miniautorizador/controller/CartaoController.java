package com.vr.miniautorizador.controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vr.miniautorizador.controller.request.CadastroCartaoRequest;
import com.vr.miniautorizador.controller.response.CadastroCartaoResponse;
import com.vr.miniautorizador.controller.response.RespostaCadastroCartao;
import com.vr.miniautorizador.service.cartao.CadastroCartaoDTO;
import com.vr.miniautorizador.service.cartao.CadastroCartaoService;
import com.vr.miniautorizador.service.cartao.ConsultaSaldoCartaoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/cartoes")
public class CartaoController {

	private final CadastroCartaoService cadastroCartaoService;
	private final ConsultaSaldoCartaoService consultaSaldoCartaoService;
	private final RespostaCadastroCartao respostaCadastroCartao;

	public CartaoController(CadastroCartaoService cadastroCartaoService, ConsultaSaldoCartaoService consultaSaldoCartaoService, RespostaCadastroCartao respostaCadastroCartao) {
		this.cadastroCartaoService = cadastroCartaoService;
		this.consultaSaldoCartaoService = consultaSaldoCartaoService;
		this.respostaCadastroCartao = respostaCadastroCartao;
	}

	@PostMapping
	public ResponseEntity<CadastroCartaoResponse> cadastrar(@Valid @RequestBody CadastroCartaoRequest request) {
		
		var response = new CadastroCartaoResponse(request.numeroCartao(), request.senha());
		var dto = new CadastroCartaoDTO(request.numeroCartao(), request.senha());
		
		var resultado = cadastroCartaoService.cadastrar(dto);
		
		return respostaCadastroCartao.criar(resultado, response);
	}

	@GetMapping("/{numeroCartao}")
	public ResponseEntity<BigDecimal> consultarSaldoCartao(@PathVariable String numeroCartao) {
		return consultaSaldoCartaoService.consultar(numeroCartao).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}
	
}
