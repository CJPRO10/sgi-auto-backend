package com.sgi.auto.clientes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CreditoRepositorio extends JpaRepository<Credito, Long> {

    @Query("SELECT c FROM Credito c WHERE c.cliente.id = :clienteId AND c.estaActivo = true")
    Optional<Credito> buscarActivoPorCliente(@Param("clienteId") Long clienteId);

    @Query("SELECT COUNT(c) > 0 FROM Credito c WHERE c.cliente.id = :clienteId AND c.estaActivo = true")
    boolean existeCreditoActivoPorCliente(@Param("clienteId") Long clienteId);
}