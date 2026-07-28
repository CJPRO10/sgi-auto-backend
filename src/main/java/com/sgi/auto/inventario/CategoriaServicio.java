package com.sgi.auto.inventario;

import com.sgi.auto.compartido.ConflictoExcepcion;
import com.sgi.auto.compartido.RecursoNoEncontradoExcepcion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriaServicio {

    private final CategoriaRepositorio categoriaRepositorio;

    @Transactional(readOnly = true)
    public List<Categoria> listarActivas() {
        return categoriaRepositorio.listarActivas();
    }

    @Transactional
    public Categoria crear(String nombre, String descripcion) {
        if (categoriaRepositorio.existePorNombre(nombre)) {
            throw new ConflictoExcepcion(
                    "Ya existe una categoría con el nombre: " + nombre);
        }
        Categoria categoria = Categoria.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .build();
        Categoria guardada = categoriaRepositorio.save(categoria);
        log.info("Categoría creada: nombre={}", nombre);
        return guardada;
    }

    @Transactional
    public void desactivar(Long id) {
        Categoria categoria = categoriaRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "Categoría no encontrada: " + id));
        categoria.setEstaActiva(false);
        categoriaRepositorio.save(categoria);
        log.info("Categoría desactivada: id={}", id);
    }
}