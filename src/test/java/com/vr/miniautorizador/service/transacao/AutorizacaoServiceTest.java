package com.vr.miniautorizador.service.transacao;

import static com.vr.miniautorizador.service.transacao.ResultadoAutorizacao.CARTAO_INEXISTENTE;
import static com.vr.miniautorizador.service.transacao.ResultadoAutorizacao.OK;
import static com.vr.miniautorizador.service.transacao.ResultadoAutorizacao.SALDO_INSUFICIENTE;
import static com.vr.miniautorizador.service.transacao.ResultadoAutorizacao.SENHA_INVALIDA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vr.miniautorizador.domain.Cartao;
import com.vr.miniautorizador.repository.CartaoRepository;

@ExtendWith(MockitoExtension.class)
class AutorizacaoServiceTest {
	
	private static final String NUMERO_CARTAO = "6549873025634501";
	private static final String SENHA = "1234";

	@Mock
	private CartaoRepository cartaoRepository;
	
	@Mock
	private PasswordEncoder codificadorDeSenha;

	private AutorizacaoService servico;
	
	private Cartao cartao;

	@BeforeEach
	void preparar() {
		servico = new AutorizacaoService(cartaoRepository, List.of(new RegraSenhaCartao(codificadorDeSenha), new RegraSaldoDisponivel()));
		cartao = new Cartao(NUMERO_CARTAO, "hash-da-senha", new BigDecimal("500.00"));
	}

	@Test
	void recusaCartaoInexistente() {
		
		when(cartaoRepository.buscarPorNumeroParaAtualizacao(NUMERO_CARTAO)).thenReturn(Optional.empty());

		var resultado = servico.autorizar(new TransacaoDTO(NUMERO_CARTAO, SENHA, new BigDecimal("10.00")));

		assertThat(resultado).isEqualTo(CARTAO_INEXISTENTE);
	}

	@Test
	void recusaSenhaInvalida() {
		
		when(cartaoRepository.buscarPorNumeroParaAtualizacao(NUMERO_CARTAO)).thenReturn(Optional.of(cartao));
		when(codificadorDeSenha.matches(SENHA, "hash-da-senha")).thenReturn(false);

		var resultado = servico.autorizar(new TransacaoDTO(NUMERO_CARTAO, SENHA, new BigDecimal("10.00")));

		assertThat(resultado).isEqualTo(SENHA_INVALIDA);
		assertThat(cartao.getSaldo()).isEqualByComparingTo("500.00");
	}

	@Test
	void recusaValorMaiorQueSaldo() {
		
		when(cartaoRepository.buscarPorNumeroParaAtualizacao(NUMERO_CARTAO)).thenReturn(Optional.of(cartao));
		when(codificadorDeSenha.matches(SENHA, "hash-da-senha")).thenReturn(true);

		var resultado = servico.autorizar(new TransacaoDTO(NUMERO_CARTAO, SENHA, new BigDecimal("500.01")));

		assertThat(resultado).isEqualTo(SALDO_INSUFICIENTE);
		assertThat(cartao.getSaldo()).isEqualByComparingTo("500.00");
	}

	@Test
	void autoriza() {
		
		when(cartaoRepository.buscarPorNumeroParaAtualizacao(NUMERO_CARTAO)).thenReturn(Optional.of(cartao));
		when(codificadorDeSenha.matches(SENHA, "hash-da-senha")).thenReturn(true);

		var resultado = servico.autorizar(new TransacaoDTO(NUMERO_CARTAO, SENHA, new BigDecimal("10.00")));

		assertThat(resultado).isEqualTo(OK);
		assertThat(cartao.getSaldo()).isEqualByComparingTo("490.00");
	}
	
}
