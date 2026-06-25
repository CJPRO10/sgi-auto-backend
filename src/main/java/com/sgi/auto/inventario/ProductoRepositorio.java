package com.sgi.auto.inventario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepositorio extends JpaRepository<Producto, Long> {

    // Búsqueda full-text con índice GIN
    @Query(value = """
            SELECT * FROM productos
            WHERE eliminado_en IS NULL
              AND esta_activo = true
              AND fn_texto_busqueda(nombre) @@ plainto_tsquery('spanish', :termino)
            ORDER BY nombre
            LIMIT 20
            """, nativeQuery = true)
    List<Producto> buscarPorNombre(@Param("termino") String termino);

    // Búsqueda por código exacto
    @Query("SELECT p FROM Producto p WHERE p.codigo = :codigo AND p.estaActivo = true AND p.eliminadoEn IS NULL")
    Optional<Producto> buscarPorCodigo(@Param("codigo") String codigo);

    // Verificar código duplicado
    @Query("SELECT COUNT(p) > 0 FROM Producto p WHERE p.codigo = :codigo AND p.eliminadoEn IS NULL")
    boolean existePorCodigo(@Param("codigo") String codigo);

    // Listar activos con paginación
    @Query("SELECT p FROM Producto p WHERE p.estaActivo = true AND p.eliminadoEn IS NULL ORDER BY p.nombre")
    Page<Producto> listarActivos(Pageable pageable);

    // Productos con stock por debajo del mínimo
    @Query("SELECT p FROM Producto p WHERE p.estaActivo = true AND p.eliminadoEn IS NULL AND p.stockActual <= p.stockMinimo")
    List<Producto> listarConStockBajoMinimo();

    // Para lista de precios
    @Query("SELECT p FROM Producto p WHERE p.estaActivo = true AND p.eliminadoEn IS NULL AND p.mostrarEnListaPrecios = true ORDER BY p.nombre")
    List<Producto> listarParaListaPrecios();
}