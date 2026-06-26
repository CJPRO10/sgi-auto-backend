package com.sgi.auto.notificaciones;

import com.sgi.auto.compartido.ApiRespuesta;
import com.sgi.auto.notificaciones.dto.NotificacionRespuestaDTO;
import com.sgi.auto.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionControlador {

    private final NotificacionServicio notificacionServicio;

    // Canal SSE persistente
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribir(@AuthenticationPrincipal Usuario usuario) {
        return notificacionServicio.suscribir(usuario.getId());
    }

    // Badge: contar no leídas
    @GetMapping("/no-leidas/count")
    public ResponseEntity<ApiRespuesta<Long>> contarNoLeidas(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                notificacionServicio.contarNoLeidas(usuario.getId())));
    }

    // Lista de no leídas
    @GetMapping("/no-leidas")
    public ResponseEntity<ApiRespuesta<List<NotificacionRespuestaDTO>>> noLeidas(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                notificacionServicio.noLeidas(usuario.getId())));
    }

    // Historial completo
    @GetMapping("/historial")
    public ResponseEntity<ApiRespuesta<Page<NotificacionRespuestaDTO>>> historial(
            @AuthenticationPrincipal Usuario usuario,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                notificacionServicio.historial(usuario.getId(), pageable)));
    }

    // Marcar una como leída
    @PatchMapping("/{id}/leida")
    public ResponseEntity<ApiRespuesta<Void>> marcarLeida(@PathVariable Long id) {
        notificacionServicio.marcarLeida(id);
        return ResponseEntity.ok(ApiRespuesta.exitoso(null, "Notificación marcada como leída"));
    }

    // Marcar todas como leídas
    @PatchMapping("/leidas")
    public ResponseEntity<ApiRespuesta<Void>> marcarTodasLeidas(
            @AuthenticationPrincipal Usuario usuario) {
        notificacionServicio.marcarTodasLeidas(usuario.getId());
        return ResponseEntity.ok(ApiRespuesta.exitoso(null, "Todas las notificaciones marcadas como leídas"));
    }
}