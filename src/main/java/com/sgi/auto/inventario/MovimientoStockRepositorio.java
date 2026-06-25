package com.sgi.auto.inventario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface MovimientoStockRepositorio extends JpaRepository<MovimientoStock, Long> {

    // Kardex por producto
    @Query("SELECT m FROM MovimientoStock m WHERE m.producto.id = :productoId ORDER BY m.creadoEn DESC")
    Page<MovimientoStock> kardexPorProducto(@Param("productoId") Long productoId, Pageable pageable);

    // Movimientos en un rango de fechas
    @Query("SELECT m FROM MovimientoStock m WHERE m.producto.id = :productoId AND m.creadoEn BETWEEN :desde AND :hasta ORDER BY m.creadoEn DESC")
    List<MovimientoStock> kardexPorProductoYFechas(
            @Param("productoId") Long productoId,
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta);
}