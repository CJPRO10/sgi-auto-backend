package com.sgi.auto.caja;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovimientoCajaRepositorio extends JpaRepository<MovimientoCaja, Long> {

    @Query("SELECT m FROM MovimientoCaja m WHERE m.sesion.id = :sesionId ORDER BY m.creadoEn ASC")
    List<MovimientoCaja> listarPorSesion(@Param("sesionId") Long sesionId);
}
