package com.vr.miniautorizador.controller;

import static com.vr.miniautorizador.config.CorrelationIdFilter.MDC_KEY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vr.miniautorizador.controller.request.TransacaoRequest;
import com.vr.miniautorizador.controller.response.RespostaTransacao;
import com.vr.miniautorizador.service.transacao.AutorizacaoService;
import com.vr.miniautorizador.service.transacao.TransacaoDTO;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

	private static final String METRICA_TRANSACOES = "miniautorizador.transacoes";
	private static final Logger LOGGER = LoggerFactory.getLogger(TransacaoController.class);

	private final MeterRegistry meterRegistry;
	private final AutorizacaoService autorizacaoService;
	private final RespostaTransacao respostaTransacao;

	public TransacaoController(AutorizacaoService autorizacaoService, RespostaTransacao respostaTransacao, MeterRegistry meterRegistry) {
		this.autorizacaoService = autorizacaoService;
		this.respostaTransacao = respostaTransacao;
		this.meterRegistry = meterRegistry;
	}

	@PostMapping
	public ResponseEntity<String> autorizar(@Valid @RequestBody TransacaoRequest request) {
		var dto = new TransacaoDTO(request.numeroCartao(), request.senhaCartao(), request.valor());
		var resultado = autorizacaoService.autorizar(dto);
		meterRegistry.counter(METRICA_TRANSACOES, "resultado", resultado.name()).increment();
		LOGGER.info("Autorização de transação concluída: correlationId={} resultado={}", MDC.get(MDC_KEY), resultado);
		return respostaTransacao.criar(resultado);
	}

}
