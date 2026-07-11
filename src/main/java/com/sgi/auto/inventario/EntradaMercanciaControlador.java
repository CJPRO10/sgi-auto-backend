package com.sgi.auto.inventario;

import com.sgi.auto.compartido.ApiRespuesta;
import com.sgi.auto.inventario.dto.EntradaMercanciaCrearDTO;
import com.sgi.auto.inventario.dto.EntradaMercanciaRespuestaDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventario/entradas")
@RequiredArgsConstructor
public class EntradaMercanciaControlador {

    private final EntradaMercanciaServicio entradaServicio;

    @PostMapping
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<EntradaMercanciaRespuestaDTO>> registrar(
            @Valid @RequestBody EntradaMercanciaCrearDTO solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRespuesta.exitoso(
                        entradaServicio.registrar(solicitud),
                        "Entrada de mercancía registrada correctamente"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<Page<EntradaMercanciaRespuestaDTO>>> listar(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(entradaServicio.listar(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<EntradaMercanciaRespuestaDTO>> obtenerPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(entradaServicio.obtenerPorId(id)));
    }
}