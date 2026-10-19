package com.vr.miniautorizador.service.transacao;

import static com.vr.miniautorizador.service.transacao.ResultadoAutorizacao.SALDO_INSUFICIENTE;
import static com.vr.miniautorizador.service.transacao.ResultadoAutorizacao.SENHA_INVALIDA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vr.miniautorizador.domain.Cartao;

@ExtendWith(MockitoExtension.class)
class RegrasAutorizacaoTest {
	
	@Mock
	private PasswordEncoder codificadorDeSenha;

	private final RegraSaldoDisponivel regraSaldo = new RegraSaldoDisponivel();

	@Test
	void senhaCorretaNaoProduzRecusaESenhaIncorretaProduzResultadoEspecifico() {
		
		var cartao = new Cartao("6549873025634501", "hash", new BigDecimal("100.00"));
		var regraSenha = new RegraSenhaCartao(codificadorDeSenha);
		when(codificadorDeSenha.matches("1234", "hash")).thenReturn(true, false);

		assertThat(regraSenha.validar(cartao, new TransacaoDTO(cartao.getNumeroCartao(), "1234", BigDecimal.ONE))).isEmpty();
		assertThat(regraSenha.validar(cartao, new TransacaoDTO(cartao.getNumeroCartao(), "1234", BigDecimal.ONE))).contains(SENHA_INVALIDA);
	}

	@Test
	void senhaNulaTambemERecusada() {
		
		var cartao = new Cartao("6549873025634501", "hash", new BigDecimal("100.00"));
		when(codificadorDeSenha.matches("", "hash")).thenReturn(false);

		assertThat(new RegraSenhaCartao(codificadorDeSenha).validar(cartao, new TransacaoDTO(cartao.getNumeroCartao(), null, BigDecimal.ONE))).contains(SENHA_INVALIDA);
	}

	@Test
	void regraDeSaldoAceitaValorDentroDoSaldo() {
		
		var cartao = new Cartao("6549873025634501", "hash", new BigDecimal("100.00"));

		assertThat(regraSaldo.validar(cartao, new TransacaoDTO(cartao.getNumeroCartao(), "1234", new BigDecimal("100.00")))).isEmpty();
	}

	@Test
	void regraDeSaldoRecusaValorAusenteNaoPositivoOuAcimaDoSaldo() {
		
		var cartao = new Cartao("6549873025634501", "hash", new BigDecimal("100.00"));

		assertThat(regraSaldo.validar(cartao, new TransacaoDTO(cartao.getNumeroCartao(), "1234", null))).contains(SALDO_INSUFICIENTE);
		assertThat(regraSaldo.validar(cartao, new TransacaoDTO(cartao.getNumeroCartao(), "1234", BigDecimal.ZERO))).contains(SALDO_INSUFICIENTE);
		assertThat(regraSaldo.validar(cartao, new TransacaoDTO(cartao.getNumeroCartao(), "1234", new BigDecimal("100.01")))).contains(SALDO_INSUFICIENTE);
	}
	
}
