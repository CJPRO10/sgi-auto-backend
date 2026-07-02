package com.sgi.auto.clientes;

import com.sgi.auto.clientes.dto.*;
import com.sgi.auto.compartido.ApiRespuesta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DUENO','CAJERA')")
public class FidelizacionControlador {

    private final FidelizacionServicio fidelizacionServicio;

    // ── Puntos ────────────────────────────────────────────────

    @GetMapping("/{clienteId}/puntos")
    public ResponseEntity<ApiRespuesta<Page<HistorialPuntosRespuestaDTO>>> historialPuntos(
            @PathVariable Long clienteId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                fidelizacionServicio.historialPuntos(clienteId, pageable)));
    }

    @PatchMapping("/{clienteId}/puntos/ajuste")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<ClienteRespuestaDTO>> ajustarPuntos(
            @PathVariable Long clienteId,
            @Valid @RequestBody AjustePuntosDTO solicitud) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                fidelizacionServicio.ajustarPuntos(clienteId, solicitud),
                "Puntos ajustados correctamente"));
    }

    // ── Crédito ───────────────────────────────────────────────

    @PostMapping("/{clienteId}/credito")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<CreditoRespuestaDTO>> habilitarCredito(
            @PathVariable Long clienteId,
            @Valid @RequestBody HabilitarCreditoDTO solicitud) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                fidelizacionServicio.habilitarCredito(clienteId, solicitud),
                "Crédito habilitado correctamente"));
    }

    @PostMapping("/{clienteId}/credito/deuda")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<CreditoRespuestaDTO>> agregarDeuda(
            @PathVariable Long clienteId,
            @Valid @RequestBody HabilitarCreditoDTO solicitud) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                fidelizacionServicio.agregarDeudaManual(clienteId, solicitud),
                "Deuda registrada correctamente"));
    }

    @GetMapping("/{clienteId}/credito")
    public ResponseEntity<ApiRespuesta<CreditoRespuestaDTO>> obtenerCredito(
            @PathVariable Long clienteId) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                fidelizacionServicio.obtenerCreditoActivo(clienteId)));
    }

    @PostMapping("/{clienteId}/credito/pagos")
    public ResponseEntity<ApiRespuesta<CreditoRespuestaDTO>> registrarPago(
            @PathVariable Long clienteId,
            @Valid @RequestBody PagoCreditoDTO solicitud) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                fidelizacionServicio.registrarPago(clienteId, solicitud),
                "Pago registrado correctamente"));
    }
}