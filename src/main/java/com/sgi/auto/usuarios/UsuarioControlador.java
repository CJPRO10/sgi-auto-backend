package com.sgi.auto.usuarios;

import com.sgi.auto.compartido.ApiRespuesta;
import com.sgi.auto.usuarios.dto.PermisosActualizarDTO;
import com.sgi.auto.usuarios.dto.UsuarioCrearDTO;
import com.sgi.auto.usuarios.dto.UsuarioRespuestaDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador de gestión de usuarios.
 * Todos los endpoints son exclusivos del rol DUEÑO.
 * RF-003, RF-004
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DUENO')")
public class UsuarioControlador {

    private final UsuarioServicio usuarioServicio;

    /**
     * POST /api/usuarios
     * RF-003 — Crear un nuevo usuario del sistema.
     */
    @PostMapping
    public ResponseEntity<ApiRespuesta<UsuarioRespuestaDTO>> crear(
            @Valid @RequestBody UsuarioCrearDTO solicitud) {

        UsuarioRespuestaDTO creado = usuarioServicio.crear(solicitud);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiRespuesta.exitoso(creado, "Usuario creado correctamente"));
    }

    /**
     * GET /api/usuarios
     * Lista todos los usuarios activos del sistema.
     */
    @GetMapping
    public ResponseEntity<ApiRespuesta<List<UsuarioRespuestaDTO>>> listarTodos() {
        return ResponseEntity.ok(ApiRespuesta.exitoso(usuarioServicio.listarTodos()));
    }

    /**
     * GET /api/usuarios/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiRespuesta<UsuarioRespuestaDTO>> obtenerPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(usuarioServicio.obtenerPorId(id)));
    }

    /**
     * PATCH /api/usuarios/{id}/permisos
     * RF-004 — Configurar permisos granulares de la cajera.
     */
    @PatchMapping("/{id}/permisos")
    public ResponseEntity<ApiRespuesta<UsuarioRespuestaDTO>> actualizarPermisos(
            @PathVariable Long id,
            @Valid @RequestBody PermisosActualizarDTO permisos) {

        UsuarioRespuestaDTO actualizado = usuarioServicio.actualizarPermisos(id, permisos);
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(actualizado, "Permisos actualizados correctamente"));
    }

    /**
     * DELETE /api/usuarios/{id}
     * Desactiva el usuario (no lo elimina físicamente).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiRespuesta<Void>> desactivar(@PathVariable Long id) {
        usuarioServicio.desactivar(id);
        return ResponseEntity.ok(ApiRespuesta.exitoso(null, "Usuario desactivado correctamente"));
    }

    /**
     * PATCH /api/usuarios/{id}/reactivar
     */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<ApiRespuesta<UsuarioRespuestaDTO>> reactivar(@PathVariable Long id) {
        UsuarioRespuestaDTO reactivado = usuarioServicio.reactivar(id);
        return ResponseEntity.ok(ApiRespuesta.exitoso(reactivado, "Usuario reactivado correctamente"));
    }
}