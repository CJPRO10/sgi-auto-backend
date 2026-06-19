package com.sgi.auto.autenticacion;

import com.sgi.auto.autenticacion.dto.LoginSolicitudDTO;
import com.sgi.auto.autenticacion.dto.TokenRespuestaDTO;
import com.sgi.auto.usuarios.Usuario;
import com.sgi.auto.usuarios.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServicio {

    private final AuthenticationManager gestorAutenticacion;
    private final JwtUtil jwtUtil;
    private final UsuarioRepositorio usuarioRepositorio;

    @Transactional
    public TokenRespuestaDTO ingresar(LoginSolicitudDTO solicitud) {
        Usuario usuario = usuarioRepositorio
                .buscarPorNombreUsuario(solicitud.nombreUsuario())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        // verificar bloqueo por intentos fallidos
        if (usuario.estaBloqueado()) {
            throw new BadCredentialsException(
                    "Cuenta bloqueada temporalmente. Intente de nuevo más tarde.");
        }

        try {
            gestorAutenticacion.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            solicitud.nombreUsuario(),
                            solicitud.contrasena()));

        } catch (AuthenticationException ex) {
            // Registrar intento fallido y posiblemente bloquear
            usuario.registrarIntentoFallido();
            usuarioRepositorio.save(usuario);
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        // Ingreso exitoso
        usuario.registrarIngresoExitoso();
        usuarioRepositorio.save(usuario);

        Map<String, Object> permisos = Map.of(
                "puedeAplicarDescuento", usuario.isPuedeAplicarDescuento(),
                "puedeAnularVenta",      usuario.isPuedeAnularVenta(),
                "puedeCerrarCaja",       usuario.isPuedeCerrarCaja(),
                "puedeVerReportes",      usuario.isPuedeVerReportes(),
                "puedeGestionarCredito", usuario.isPuedeGestionarCredito()
        );

        String token = jwtUtil.generarToken(
                usuario.getNombreUsuario(),
                usuario.getRol().name(),
                permisos);

        log.info("Ingreso exitoso: usuario={}, rol={}", usuario.getNombreUsuario(), usuario.getRol());

        return new TokenRespuestaDTO(
                token,
                usuario.getNombreCompleto(),
                usuario.getRol().name(),
                permisos);
    }
}
