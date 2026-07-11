package com.sgi.auto.inventario;

import com.sgi.auto.compartido.ApiRespuesta;
import com.sgi.auto.compartido.ConflictoExcepcion;
import com.sgi.auto.compartido.RecursoNoEncontradoExcepcion;
import com.sgi.auto.inventario.dto.ProveedorDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/proveedores")
@RequiredArgsConstructor
public class ProveedorControlador {

    private final ProveedorRepositorio proveedorRepositorio;

    @GetMapping
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<List<ProveedorDTO>>> listar() {
        List<ProveedorDTO> proveedores = proveedorRepositorio.listarActivos()
                .stream().map(this::aDTO).toList();
        return ResponseEntity.ok(ApiRespuesta.exitoso(proveedores));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<ProveedorDTO>> crear(
            @Valid @RequestBody ProveedorDTO solicitud) {
        if (solicitud.nit() != null && !solicitud.nit().isBlank()
                && proveedorRepositorio.existePorNit(solicitud.nit())) {
            throw new ConflictoExcepcion("Ya existe un proveedor con NIT: " + solicitud.nit());
        }
        Proveedor proveedor = Proveedor.builder()
                .nombre(solicitud.nombre())
                .nit(solicitud.nit())
                .telefono(solicitud.telefono())
                .correo(solicitud.correo())
                .direccion(solicitud.direccion())
                .personaContacto(solicitud.personaContacto())
                .notas(solicitud.notas())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRespuesta.exitoso(aDTO(proveedorRepositorio.save(proveedor)),
                        "Proveedor creado correctamente"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<ProveedorDTO>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorDTO solicitud) {
        Proveedor proveedor = proveedorRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "Proveedor no encontrado: " + id));
        proveedor.setNombre(solicitud.nombre());
        proveedor.setNit(solicitud.nit());
        proveedor.setTelefono(solicitud.telefono());
        proveedor.setCorreo(solicitud.correo());
        proveedor.setDireccion(solicitud.direccion());
        proveedor.setPersonaContacto(solicitud.personaContacto());
        proveedor.setNotas(solicitud.notas());
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                aDTO(proveedorRepositorio.save(proveedor)),
                "Proveedor actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<Void>> desactivar(@PathVariable Long id) {
        Proveedor proveedor = proveedorRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "Proveedor no encontrado: " + id));
        proveedor.setEstaActivo(false);
        proveedorRepositorio.save(proveedor);
        return ResponseEntity.ok(ApiRespuesta.exitoso(null, "Proveedor desactivado"));
    }

    private ProveedorDTO aDTO(Proveedor p) {
        return new ProveedorDTO(p.getId(), p.getNombre(), p.getNit(),
                p.getTelefono(), p.getCorreo(), p.getDireccion(),
                p.getPersonaContacto(), p.getNotas(), p.isEstaActivo());
    }
}