package com.sgi.auto.autenticacion;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey claveSecreta;
    private final long expiracionMs;

    public JwtUtil(
            @Value("${sgi.jwt.secreto}") String secreto,
            @Value("${sgi.jwt.expiracion-horas}") int expiracionHoras) {

        this.claveSecreta = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = (long) expiracionHoras * 60 * 60 * 1000;
    }

    public String generarToken(String nombreUsuario, String rol, Map<String, Object> permisos) {
        return Jwts.builder()
                .subject(nombreUsuario)
                .claim("rol", rol)
                .claim("permisos", permisos)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiracionMs))
                .signWith(claveSecreta)
                .compact();
    }

    public String extraerNombreUsuario(String token) {
        return parsear(token).getSubject();
    }

    public String extraerRol(String token) {
        return parsear(token).get("rol", String.class);
    }

    public boolean esValido(String token) {
        try {
            parsear(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token JWT inválido: {}", ex.getMessage());
            return false;
        }
    }

    private Claims parsear(String token) {
        return Jwts.parser()
                .verifyWith(claveSecreta)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}