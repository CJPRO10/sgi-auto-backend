package com.sgi.auto.clientes;

import com.sgi.auto.clientes.dto.ClienteActualizarDTO;
import com.sgi.auto.clientes.dto.ClienteCrearDTO;
import com.sgi.auto.clientes.dto.ClienteRespuestaDTO;
import com.sgi.auto.compartido.ApiRespuesta;
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
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteControlador {

    private final ClienteServicio clienteServicio;

    @PostMapping
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<ClienteRespuestaDTO>> crear(
            @Valid @RequestBody ClienteCrearDTO solicitud) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiRespuesta.exitoso(
                        clienteServicio.crear(solicitud),
                        "Cliente registrado correctamente"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DUENO','CAJERA','MECANICO')")
    public ResponseEntity<ApiRespuesta<Page<ClienteRespuestaDTO>>> listar(
            @PageableDefault(size = 20, sort = "nombreCompleto") Pageable pageable) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(clienteServicio.listarTodos(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DUENO','CAJERA','MECANICO')")
    public ResponseEntity<ApiRespuesta<ClienteRespuestaDTO>> obtenerPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(clienteServicio.obtenerPorId(id)));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('DUENO','CAJERA','MECANICO')")
    public ResponseEntity<ApiRespuesta<List<ClienteRespuestaDTO>>> buscar(
            @RequestParam String q) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(clienteServicio.buscar(q)));
    }

    @GetMapping("/identificacion/{numero}")
    @PreAuthorize("hasAnyRole('DUENO','CAJERA','MECANICO')")
    public ResponseEntity<ApiRespuesta<ClienteRespuestaDTO>> obtenerPorIdentificacion(
            @PathVariable String numero) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(
                        clienteServicio.obtenerPorIdentificacion(numero)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<ClienteRespuestaDTO>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteActualizarDTO solicitud) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(
                        clienteServicio.actualizar(id, solicitud),
                        "Cliente actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<Void>> eliminar(@PathVariable Long id) {
        clienteServicio.eliminar(id);
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(null, "Cliente eliminado correctamente"));
    }
}