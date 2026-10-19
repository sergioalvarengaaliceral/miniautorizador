package com.vr.miniautorizador.controller.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;

public record TransacaoRequest(String numeroCartao, String senhaCartao, @Digits(integer = 10, fraction = 2) BigDecimal valor) { }
