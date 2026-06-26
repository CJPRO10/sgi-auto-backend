package com.sgi.auto.clientes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistorialPuntosRepositorio extends JpaRepository<HistorialPuntos, Long> {

    @Query("SELECT h FROM HistorialPuntos h WHERE h.cliente.id = :clienteId ORDER BY h.creadoEn DESC")
    Page<HistorialPuntos> listarPorCliente(@Param("clienteId") Long clienteId, Pageable pageable);
}