package com.sgi.auto.inventario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProveedorRepositorio extends JpaRepository<Proveedor, Long> {

    @Query("SELECT p FROM Proveedor p WHERE p.estaActivo = true ORDER BY p.nombre")
    List<Proveedor> listarActivos();

    @Query("SELECT COUNT(p) > 0 FROM Proveedor p WHERE p.nit = :nit AND p.estaActivo = true")
    boolean existePorNit(String nit);
}