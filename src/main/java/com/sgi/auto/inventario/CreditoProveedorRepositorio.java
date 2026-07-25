package com.sgi.auto.inventario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CreditoProveedorRepositorio extends JpaRepository<CreditoProveedor, Long> {

    @Query("SELECT c FROM CreditoProveedor c WHERE c.estaActivo = true ORDER BY c.creadoEn DESC")
    List<CreditoProveedor> listarActivos();

    @Query("SELECT c FROM CreditoProveedor c WHERE c.proveedor.id = :proveedorId AND c.estaActivo = true")
    Optional<CreditoProveedor> buscarActivoPorProveedor(Long proveedorId);
}