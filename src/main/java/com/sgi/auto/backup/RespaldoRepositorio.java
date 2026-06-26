package com.sgi.auto.backup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RespaldoRepositorio extends JpaRepository<Respaldo, Long> {

    @Query("SELECT r FROM Respaldo r ORDER BY r.creadoEn DESC")
    Page<Respaldo> listarTodos(Pageable pageable);

    @Query("SELECT r FROM Respaldo r WHERE r.exitoso = true ORDER BY r.creadoEn DESC")
    Page<Respaldo> listarExitosos(Pageable pageable);
}