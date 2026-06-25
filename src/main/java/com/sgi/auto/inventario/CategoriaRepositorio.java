package com.sgi.auto.inventario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {

    @Query("SELECT c FROM Categoria c WHERE c.estaActiva = true ORDER BY c.nombre")
    List<Categoria> listarActivas();

    @Query("SELECT COUNT(c) > 0 FROM Categoria c WHERE c.nombre = :nombre AND c.estaActiva = true")
    boolean existePorNombre(String nombre);
}