package com.vr.miniautorizador.controller.request;

import jakarta.validation.constraints.NotBlank;

public record CadastroCartaoRequest(
    @NotBlank String numeroCartao,
    @NotBlank String senha
) { }
