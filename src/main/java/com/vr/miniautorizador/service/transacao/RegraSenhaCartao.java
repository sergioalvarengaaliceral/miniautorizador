package com.vr.miniautorizador.service.transacao;

import static com.vr.miniautorizador.service.transacao.ResultadoAutorizacao.SENHA_INVALIDA;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.vr.miniautorizador.domain.Cartao;

@Component
@Order(1)
public class RegraSenhaCartao implements RegraAutorizacao {

	private final PasswordEncoder codificadorDeSenha;

	public RegraSenhaCartao(PasswordEncoder codificadorDeSenha) {
		this.codificadorDeSenha = codificadorDeSenha;
	}

	@Override
	public Optional<ResultadoAutorizacao> validar(Cartao cartao, TransacaoDTO transacao) {
		return Stream.of(Objects.requireNonNullElse(transacao.senhaCartao(), "")).filter(senha -> !codificadorDeSenha.matches(senha, cartao.getSenhaHash())).map(senha -> SENHA_INVALIDA).findFirst();
	}

}
