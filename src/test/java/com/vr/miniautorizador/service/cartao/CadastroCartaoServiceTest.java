package com.vr.miniautorizador.service.cartao;

import static com.vr.miniautorizador.service.cartao.ResultadoCadastroCartao.CADASTRADO;
import static com.vr.miniautorizador.service.cartao.ResultadoCadastroCartao.JA_EXISTENTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vr.miniautorizador.domain.Cartao;
import com.vr.miniautorizador.repository.CartaoRepository;

@ExtendWith(MockitoExtension.class)
class CadastroCartaoServiceTest {
	
	private static final String NUMERO_CARTAO = "6549873025634501";
	private static final String SENHA = "1234";

	@Mock
	private CartaoRepository cartaoRepository;
	
	@Mock
	private PasswordEncoder codificadorDeSenha;
	
	@InjectMocks
	private CadastroCartaoService servico;

	@Test
	void retornaExistenteQuandoNumeroJaCadastrado() {
		
		when(cartaoRepository.existsById(NUMERO_CARTAO)).thenReturn(true);

		var resultado = servico.cadastrar(new CadastroCartaoDTO(NUMERO_CARTAO, SENHA));

		assertThat(resultado).isEqualTo(JA_EXISTENTE);
		verify(cartaoRepository, never()).save(any(Cartao.class));
	}

	@Test
	void persisteCartaoComSaldoInicial() {
		
		when(cartaoRepository.existsById(NUMERO_CARTAO)).thenReturn(false);
		when(codificadorDeSenha.encode(SENHA)).thenReturn("hash-da-senha");

		var resultado = servico.cadastrar(new CadastroCartaoDTO(NUMERO_CARTAO, SENHA));

		var cartaoSalvo = ArgumentCaptor.forClass(Cartao.class);
		verify(cartaoRepository).save(cartaoSalvo.capture());
		assertThat(resultado).isEqualTo(CADASTRADO);
		assertThat(cartaoSalvo.getValue().getNumeroCartao()).isEqualTo(NUMERO_CARTAO);
		assertThat(cartaoSalvo.getValue().getSenhaHash()).isEqualTo("hash-da-senha");
		assertThat(cartaoSalvo.getValue().getSaldo()).isEqualByComparingTo(new BigDecimal("500.00"));
	}
	
}
