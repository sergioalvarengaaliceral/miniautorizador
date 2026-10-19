package com.vr.miniautorizador.service.transacao;

import java.util.Optional;

import com.vr.miniautorizador.domain.Cartao;

public interface RegraAutorizacao {
	
    Optional<ResultadoAutorizacao> validar(Cartao cartao, TransacaoDTO transacao);

}
