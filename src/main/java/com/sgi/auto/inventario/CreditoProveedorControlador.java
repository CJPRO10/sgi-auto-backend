package com.sgi.auto.inventario;

import com.sgi.auto.compartido.ApiRespuesta;
import com.sgi.auto.inventario.dto.CreditoProveedorCrearDTO;
import com.sgi.auto.inventario.dto.CreditoProveedorRespuestaDTO;
import com.sgi.auto.inventario.dto.PagoCreditoProveedorDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/creditos-proveedor")
@RequiredArgsConstructor
public class CreditoProveedorControlador {

    private final CreditoProveedorServicio creditoServicio;

    @PostMapping
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<CreditoProveedorRespuestaDTO>> crear(
            @Valid @RequestBody CreditoProveedorCrearDTO solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRespuesta.exitoso(
                        creditoServicio.crear(solicitud),
                        "Crédito con proveedor registrado correctamente"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<List<CreditoProveedorRespuestaDTO>>> listar() {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(creditoServicio.listarActivos()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<CreditoProveedorRespuestaDTO>> obtenerPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(creditoServicio.obtenerPorId(id)));
    }

    @PostMapping("/{id}/pagos")
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<CreditoProveedorRespuestaDTO>> registrarPago(
            @PathVariable Long id,
            @Valid @RequestBody PagoCreditoProveedorDTO solicitud) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                creditoServicio.registrarPago(id, solicitud),
                "Pago registrado correctamente"));
    }
}