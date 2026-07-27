package com.sgi.auto.inventario;

import com.sgi.auto.compartido.ApiRespuesta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/categorias")
@RequiredArgsConstructor
public class CategoriaControlador {

    private final CategoriaServicio categoriaServicio;

    @GetMapping
    @PreAuthorize("hasAnyRole('DUENO','CAJERA','MECANICO')")
    public ResponseEntity<ApiRespuesta<List<Categoria>>> listar() {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(categoriaServicio.listarActivas()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<Categoria>> crear(
            @Valid @RequestBody CategoriaCrearDTO solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRespuesta.exitoso(
                        categoriaServicio.crear(solicitud.nombre(), solicitud.descripcion()),
                        "Categoría creada correctamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<Void>> desactivar(@PathVariable Long id) {
        categoriaServicio.desactivar(id);
        return ResponseEntity.ok(ApiRespuesta.exitoso(null, "Categoría desactivada"));
    }

    public record CategoriaCrearDTO(
            @NotBlank String nombre,
            String descripcion
    ) {}
}