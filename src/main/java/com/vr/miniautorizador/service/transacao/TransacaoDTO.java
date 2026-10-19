package com.vr.miniautorizador.service.transacao;

import java.math.BigDecimal;

public record TransacaoDTO(String numeroCartao, String senhaCartao, BigDecimal valor) { }
