package com.vr.miniautorizador.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CartaoTest {
	
	@Test
	void permiteDebitoPositivoAteOSaldoDisponivel() {
		
		var cartao = new Cartao("6549873025634501", "hash", new BigDecimal("100.00"));

		assertThat(cartao.podeDebitar(new BigDecimal("100.00"))).isTrue();
		assertThat(cartao.podeDebitar(new BigDecimal("99.99"))).isTrue();
		assertThat(cartao.podeDebitar(new BigDecimal("100.01"))).isFalse();
		assertThat(cartao.podeDebitar(new BigDecimal("0.001"))).isTrue();
		assertThat(cartao.podeDebitar(BigDecimal.ZERO)).isFalse();
		assertThat(cartao.podeDebitar(null)).isFalse();
	}

	@Test
	void debitarAtualizaSaldo() {
		
		var cartao = new Cartao("6549873025634501", "hash", new BigDecimal("100.00"));

		cartao.debitar(new BigDecimal("35.25"));

		assertThat(cartao.getSaldo()).isEqualByComparingTo("64.75");
	}

	@Test
	void debitarRecusaValorInvalidoSemAlterarSaldo() {
		
		var cartao = new Cartao("6549873025634501", "hash", new BigDecimal("100.00"));

		assertThatThrownBy(() -> cartao.debitar(new BigDecimal("100.01"))).isInstanceOf(IllegalArgumentException.class);
		assertThat(cartao.getSaldo()).isEqualByComparingTo("100.00");
	}
	
}
