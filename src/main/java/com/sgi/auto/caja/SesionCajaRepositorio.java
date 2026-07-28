package com.sgi.auto.caja;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SesionCajaRepositorio extends JpaRepository<SesionCaja, Long> {

    // Sesión abierta de un usuario específico
    @Query("SELECT s FROM SesionCaja s WHERE s.estaAbierta = true AND s.cajera.id = :cajeraId")
    Optional<SesionCaja> buscarSesionAbiertaPorCajera(@Param("cajeraId") Long cajeraId);

    // Todas las sesiones abiertas (vista admin)
    @Query("SELECT s FROM SesionCaja s WHERE s.estaAbierta = true ORDER BY s.abiertaEn DESC")
    List<SesionCaja> listarSesionesAbiertas();

    // Historial paginado
    @Query("SELECT s FROM SesionCaja s ORDER BY s.abiertaEn DESC")
    Page<SesionCaja> listarTodas(Pageable pageable);

    // Sesiones de una cajera específica
    @Query("SELECT s FROM SesionCaja s WHERE s.cajera.id = :cajeraId ORDER BY s.abiertaEn DESC")
    Page<SesionCaja> listarPorCajera(@Param("cajeraId") Long cajeraId, Pageable pageable);
}