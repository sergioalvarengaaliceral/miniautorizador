package com.vr.miniautorizador.service.cartao;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vr.miniautorizador.domain.Cartao;
import com.vr.miniautorizador.repository.CartaoRepository;

@Service
public class ConsultaSaldoCartaoService {

	private final CartaoRepository cartaoRepository;

	public ConsultaSaldoCartaoService(CartaoRepository cartaoRepository) {
		this.cartaoRepository = cartaoRepository;
	}

	@Transactional(readOnly = true)
	public Optional<BigDecimal> consultar(String numeroCartao) {
		return cartaoRepository.findById(numeroCartao).map(Cartao::getSaldo);
	}

}
