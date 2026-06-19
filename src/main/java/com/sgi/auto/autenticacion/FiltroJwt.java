package com.sgi.auto.autenticacion;

import com.sgi.auto.usuarios.UsuarioRepositorio;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class FiltroJwt extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioRepositorio usuarioRepositorio;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest solicitud,
            @NonNull HttpServletResponse respuesta,
            @NonNull FilterChain cadena) throws ServletException, IOException {

        final String cabecera = solicitud.getHeader("Authorization");

        // Si no hay cabecera Authorization o no empieza con "Bearer ", continúa sin autenticar
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            cadena.doFilter(solicitud, respuesta);
            return;
        }

        final String token = cabecera.substring(7); // quita "Bearer "

        if (!jwtUtil.esValido(token)) {
            cadena.doFilter(solicitud, respuesta);
            return;
        }

        String nombreUsuario = jwtUtil.extraerNombreUsuario(token);

        // Solo autenticamos si aún no hay autenticación en el contexto
        if (nombreUsuario != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails usuario = usuarioRepositorio
                    .buscarPorNombreUsuario(nombreUsuario)
                    .orElse(null);

            if (usuario != null && usuario.isEnabled()) {
                UsernamePasswordAuthenticationToken autenticacion =
                        new UsernamePasswordAuthenticationToken(
                                usuario,
                                null,
                                usuario.getAuthorities());

                autenticacion.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(solicitud));

                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            }
        }

        cadena.doFilter(solicitud, respuesta);
    }
}