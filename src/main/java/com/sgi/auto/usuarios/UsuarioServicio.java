package com.sgi.auto.usuarios;

import com.sgi.auto.compartido.ConflictoExcepcion;
import com.sgi.auto.compartido.RecursoNoEncontradoExcepcion;
import com.sgi.auto.usuarios.dto.PermisosActualizarDTO;
import com.sgi.auto.usuarios.dto.UsuarioCrearDTO;
import com.sgi.auto.usuarios.dto.UsuarioMapper;
import com.sgi.auto.usuarios.dto.UsuarioRespuestaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de gestión de usuarios del sistema.
 * RF-003 — Creación y gestión de usuarios por el DUEÑO.
 * RF-004 — Configuración de permisos granulares de la CAJERA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServicio {

    private final UsuarioRepositorio usuarioRepositorio;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder codificadorContrasena;

    /**
     * Crea un nuevo usuario del sistema.
     * RF-003
     */
    @Transactional
    public UsuarioRespuestaDTO crear(UsuarioCrearDTO solicitud) {

        if (usuarioRepositorio.existePorNombreUsuario(solicitud.nombreUsuario())) {
            throw new ConflictoExcepcion(
                    "Ya existe un usuario con el nombre de usuario: " + solicitud.nombreUsuario());
        }

        Usuario usuario = usuarioMapper.aEntidad(solicitud);
        usuario.setContrasenaHash(codificadorContrasena.encode(solicitud.contrasena()));

        // RF-004: los permisos granulares solo aplican a CAJERA
        if (usuario.getRol() != RolUsuario.CAJERA) {
            usuario.setPuedeAplicarDescuento(false);
            usuario.setPuedeAnularVenta(false);
            usuario.setPuedeCerrarCaja(false);
            usuario.setPuedeVerReportes(false);
            usuario.setPuedeGestionarCredito(false);
        }

        Usuario guardado = usuarioRepositorio.save(usuario);
        log.info("Usuario creado: nombreUsuario={}, rol={}",
                guardado.getNombreUsuario(), guardado.getRol());

        return usuarioMapper.aDTO(guardado);
    }

    /**
     * Lista todos los usuarios activos (no eliminados).
     */
    @Transactional(readOnly = true)
    public List<UsuarioRespuestaDTO> listarTodos() {
        return usuarioRepositorio.findAll().stream()
                .filter(u -> !u.estaEliminado())
                .map(usuarioMapper::aDTO)
                .toList();
    }

    /**
     * Obtiene un usuario por su id.
     */
    @Transactional(readOnly = true)
    public UsuarioRespuestaDTO obtenerPorId(Long id) {
        Usuario usuario = buscarOLanzar(id);
        return usuarioMapper.aDTO(usuario);
    }

    /**
     * Actualiza los permisos granulares de un usuario CAJERA.
     * RF-004
     */
    @Transactional
    public UsuarioRespuestaDTO actualizarPermisos(Long id, PermisosActualizarDTO permisos) {
        Usuario usuario = buscarOLanzar(id);

        if (usuario.getRol() != RolUsuario.CAJERA) {
            throw new com.sgi.auto.compartido.ReglaNegocioExcepcion(
                    "Los permisos granulares solo se pueden configurar para usuarios con rol CAJERA");
        }

        usuario.setPuedeAplicarDescuento(permisos.puedeAplicarDescuento());
        usuario.setPuedeAnularVenta(permisos.puedeAnularVenta());
        usuario.setPuedeCerrarCaja(permisos.puedeCerrarCaja());
        usuario.setPuedeVerReportes(permisos.puedeVerReportes());
        usuario.setPuedeGestionarCredito(permisos.puedeGestionarCredito());

        Usuario actualizado = usuarioRepositorio.save(usuario);
        log.info("Permisos actualizados para usuario: nombreUsuario={}", actualizado.getNombreUsuario());

        return usuarioMapper.aDTO(actualizado);
    }

    /**
     * Desactiva un usuario (soft delete lógico vía estaActivo, no elimina el registro).
     */
    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = buscarOLanzar(id);
        usuario.setEstaActivo(false);
        usuarioRepositorio.save(usuario);
        log.info("Usuario desactivado: nombreUsuario={}", usuario.getNombreUsuario());
    }

    /**
     * Reactiva un usuario previamente desactivado.
     */
    @Transactional
    public UsuarioRespuestaDTO reactivar(Long id) {
        Usuario usuario = buscarOLanzar(id);
        usuario.setEstaActivo(true);
        Usuario actualizado = usuarioRepositorio.save(usuario);
        return usuarioMapper.aDTO(actualizado);
    }

    // ── Helper privado ──────────────────────────────────────────

    private Usuario buscarOLanzar(Long id) {
        return usuarioRepositorio.findById(id)
                .filter(u -> !u.estaEliminado())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "No se encontró el usuario con id: " + id));
    }
}
