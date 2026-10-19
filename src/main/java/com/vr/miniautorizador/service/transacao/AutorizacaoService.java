package com.vr.miniautorizador.service.transacao;

import static com.vr.miniautorizador.service.transacao.ResultadoAutorizacao.CARTAO_INEXISTENTE;
import static com.vr.miniautorizador.service.transacao.ResultadoAutorizacao.OK;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vr.miniautorizador.domain.Cartao;
import com.vr.miniautorizador.repository.CartaoRepository;

@Service
public class AutorizacaoService {

	private final CartaoRepository cartaoRepository;
	private final List<RegraAutorizacao> regrasAutorizacao;

	public AutorizacaoService(CartaoRepository cartaoRepository, List<RegraAutorizacao> regrasAutorizacao) {
		this.cartaoRepository = cartaoRepository;
		this.regrasAutorizacao = regrasAutorizacao;
	}

	@Transactional
	public ResultadoAutorizacao autorizar(TransacaoDTO transacao) {
		return cartaoRepository.buscarPorNumeroParaAtualizacao(transacao.numeroCartao()).map(cartao -> avaliarRegras(cartao, transacao)).orElse(CARTAO_INEXISTENTE);
	}

	private ResultadoAutorizacao avaliarRegras(Cartao cartao, TransacaoDTO transacao) {
		return regrasAutorizacao.stream().map(regra -> regra.validar(cartao, transacao)).flatMap(Optional::stream).findFirst().orElseGet(() -> debitar(cartao, transacao));
	}

	private ResultadoAutorizacao debitar(Cartao cartao, TransacaoDTO transacao) {
		cartao.debitar(transacao.valor());
		return OK;
	}

}
