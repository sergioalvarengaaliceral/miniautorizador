package com.vr.miniautorizador.service.cartao;

import static com.vr.miniautorizador.service.cartao.ResultadoCadastroCartao.CADASTRADO;
import static com.vr.miniautorizador.service.cartao.ResultadoCadastroCartao.JA_EXISTENTE;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vr.miniautorizador.domain.Cartao;
import com.vr.miniautorizador.repository.CartaoRepository;

@Service
public class CadastroCartaoService {
	
    private static final BigDecimal SALDO_INICIAL = new BigDecimal("500.00");

    private final CartaoRepository cartaoRepository;
    private final PasswordEncoder codificadorDeSenha;

    public CadastroCartaoService(CartaoRepository cartaoRepository, PasswordEncoder codificadorDeSenha) {
        this.cartaoRepository = cartaoRepository;
        this.codificadorDeSenha = codificadorDeSenha;
    }

    @Transactional
    public ResultadoCadastroCartao cadastrar(CadastroCartaoDTO dto) {
       return Optional.of(dto).filter(cartao -> !cartaoRepository.existsById(cartao.numeroCartao())).map(this::persistir).orElse(JA_EXISTENTE);
    }

    private ResultadoCadastroCartao persistir(CadastroCartaoDTO dto) {
        var cartao = new Cartao(dto.numeroCartao(), codificadorDeSenha.encode(dto.senha()), SALDO_INICIAL);
        cartaoRepository.save(cartao);
        return CADASTRADO;
    }
    
}
