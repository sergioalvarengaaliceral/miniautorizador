package com.vr.miniautorizador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.vr.miniautorizador.domain.Cartao;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface CartaoRepository extends JpaRepository<Cartao, String> {
    
	@Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select c from Cartao c where c.numeroCartao = :numeroCartao")
    Optional<Cartao> buscarPorNumeroParaAtualizacao(@Param("numeroCartao") String numeroCartao);
	
}
