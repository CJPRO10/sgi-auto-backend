package com.sgi.auto.inventario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntradaMercanciaRepositorio
        extends JpaRepository<EntradaMercancia, Long> {
    Page<EntradaMercancia> findAllByOrderByCreadoEnDesc(Pageable pageable);
}