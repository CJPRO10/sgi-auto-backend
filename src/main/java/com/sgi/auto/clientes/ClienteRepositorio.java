package com.sgi.auto.clientes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {

    // Búsqueda full-text con índice GIN
    @Query(value = """
            SELECT * FROM clientes
            WHERE eliminado_en IS NULL
              AND fn_texto_busqueda(nombre_completo) @@ plainto_tsquery('spanish', :termino)
            ORDER BY nombre_completo
            LIMIT 20
            """, nativeQuery = true)
    java.util.List<Cliente> buscarPorNombre(@Param("termino") String termino);

    // Búsqueda por número de identificación exacto
    @Query("SELECT c FROM Cliente c WHERE c.numeroIdentificacion = :numero AND c.eliminadoEn IS NULL")
    Optional<Cliente> buscarPorNumeroIdentificacion(@Param("numero") String numero);

    // Verificar duplicado antes de crear
    @Query("SELECT COUNT(c) > 0 FROM Cliente c WHERE c.numeroIdentificacion = :numero AND c.eliminadoEn IS NULL")
    boolean existePorNumeroIdentificacion(@Param("numero") String numero);

    // Listar todos activos con paginación
    @Query("SELECT c FROM Cliente c WHERE c.eliminadoEn IS NULL ORDER BY c.nombreCompleto")
    Page<Cliente> listarActivos(Pageable pageable);
}