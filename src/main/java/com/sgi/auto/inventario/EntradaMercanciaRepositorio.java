package com.sgi.auto.inventario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntradaMercanciaRepositorio extends JpaRepository<EntradaMercancia, Long> {

    @Query("SELECT e FROM EntradaMercancia e ORDER BY e.creadoEn DESC")
    Page<EntradaMercancia> listarTodas(Pageable pageable);
}