package com.sgi.auto.taller;

import com.sgi.auto.compartido.ApiRespuesta;
import com.sgi.auto.taller.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/taller/ordenes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DUENO','MECANICO')")
public class TallerControlador {

    private final TallerServicio tallerServicio;

    @PostMapping
    @PreAuthorize("hasAnyRole('DUENO','MECANICO')")
    public ResponseEntity<ApiRespuesta<OTRespuestaDTO>> crear(
            @Valid @RequestBody OTCrearDTO solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRespuesta.exitoso(
                        tallerServicio.crear(solicitud),
                        "Orden de trabajo creada correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiRespuesta<OTRespuestaDTO>> obtenerPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(tallerServicio.obtenerPorId(id)));
    }

    @GetMapping
    public ResponseEntity<ApiRespuesta<Page<OTRespuestaDTO>>> listarActivas(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(tallerServicio.listarActivas(pageable)));
    }

    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('DUENO', 'MECANICO')")
    public ResponseEntity<ApiRespuesta<Page<OTRespuestaDTO>>> listarTodas(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(tallerServicio.listarTodas(pageable)));
    }

    @GetMapping("/placa/{placa}")
    public ResponseEntity<ApiRespuesta<List<OTRespuestaDTO>>> porPlaca(
            @PathVariable String placa) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(tallerServicio.buscarPorPlaca(placa)));
    }

    @GetMapping("/mecanico/{mecanicoId}")
    public ResponseEntity<ApiRespuesta<List<OTRespuestaDTO>>> porMecanico(
            @PathVariable Long mecanicoId) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                tallerServicio.otActivasPorMecanico(mecanicoId)));
    }

    @PostMapping("/{id}/servicios")
    public ResponseEntity<ApiRespuesta<OTRespuestaDTO>> agregarServicio(
            @PathVariable Long id,
            @Valid @RequestBody ServicioOTDTO solicitud) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                tallerServicio.agregarServicio(id, solicitud),
                "Servicio agregado correctamente"));
    }

    @DeleteMapping("/{id}/servicios/{servicioId}")
    public ResponseEntity<ApiRespuesta<OTRespuestaDTO>> eliminarServicio(
            @PathVariable Long id,
            @PathVariable Long servicioId) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                tallerServicio.eliminarServicio(id, servicioId),
                "Servicio eliminado correctamente"));
    }

    @PostMapping("/{id}/repuestos")
    public ResponseEntity<ApiRespuesta<OTRespuestaDTO>> agregarRepuesto(
            @PathVariable Long id,
            @Valid @RequestBody RepuestoOTDTO solicitud) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                tallerServicio.agregarRepuesto(id, solicitud),
                "Repuesto agregado y stock descontado correctamente"));
    }

    @DeleteMapping("/{id}/repuestos/{repuestoId}")
    public ResponseEntity<ApiRespuesta<OTRespuestaDTO>> eliminarRepuesto(
            @PathVariable Long id,
            @PathVariable Long repuestoId) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                tallerServicio.eliminarRepuesto(id, repuestoId),
                "Repuesto eliminado y stock devuelto correctamente"));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiRespuesta<OTRespuestaDTO>> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoDTO solicitud) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                tallerServicio.cambiarEstado(id, solicitud),
                "Estado actualizado correctamente"));
    }
}