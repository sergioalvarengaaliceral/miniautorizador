package com.vr.miniautorizador.domain;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cartoes")
public class Cartao {

	@Id
	@Column(name = "numero_cartao", length = 16, nullable = false)
	private String numeroCartao;

	@Column(name = "senha_hash", nullable = false)
	private String senhaHash;

	@Column(name = "saldo", precision = 12, scale = 2, nullable = false)
	private BigDecimal saldo;

	protected Cartao() {
	}

	public Cartao(String numeroCartao, String senhaHash, BigDecimal saldo) {
		this.numeroCartao = numeroCartao;
		this.senhaHash = senhaHash;
		this.saldo = saldo;
	}

	public String getNumeroCartao() {
		return numeroCartao;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public BigDecimal getSaldo() {
		return saldo;
	}

	public boolean podeDebitar(BigDecimal valor) {
		return Optional.ofNullable(valor).filter(valorInformado -> valorInformado.signum() > 0 && saldo.compareTo(valorInformado) >= 0).isPresent();
	}

	public void debitar(BigDecimal valor) {
		this.saldo = Optional.ofNullable(valor).filter(this::podeDebitar).map(this.saldo::subtract).orElseThrow(() -> new IllegalArgumentException("O cartão não possui saldo suficiente para o débito."));
	}
	
}
