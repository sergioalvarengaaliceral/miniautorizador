package com.vr.miniautorizador.service.transacao;

import static com.vr.miniautorizador.service.transacao.ResultadoAutorizacao.SALDO_INSUFICIENTE;

import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.vr.miniautorizador.domain.Cartao;

@Component
@Order(2)
public class RegraSaldoDisponivel implements RegraAutorizacao {

	@Override
	public Optional<ResultadoAutorizacao> validar(Cartao cartao, TransacaoDTO transacao) {
		return Stream.of(transacao).filter(dados -> !cartao.podeDebitar(dados.valor())).map(dados -> SALDO_INSUFICIENTE).findFirst();
	}

}
