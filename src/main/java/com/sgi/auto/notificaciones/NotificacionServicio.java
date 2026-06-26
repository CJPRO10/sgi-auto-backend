package com.sgi.auto.notificaciones;

import com.sgi.auto.notificaciones.dto.NotificacionRespuestaDTO;
import com.sgi.auto.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionServicio {

    private final NotificacionRepositorio notificacionRepositorio;

    // Mapa de emisores SSE activos: usuarioId → SseEmitter
    private final Map<Long, SseEmitter> emisoresActivos = new ConcurrentHashMap<>();

    // ── SSE — Canal persistente ──────────────────────
    public SseEmitter suscribir(Long usuarioId) {
        SseEmitter emisor = new SseEmitter(Long.MAX_VALUE);

        emisoresActivos.put(usuarioId, emisor);

        emisor.onCompletion(() -> emisoresActivos.remove(usuarioId));
        emisor.onTimeout(() -> emisoresActivos.remove(usuarioId));
        emisor.onError(e -> emisoresActivos.remove(usuarioId));

        // Enviar notificaciones no leídas pendientes al conectarse
        try {
            List<Notificacion> pendientes =
                    notificacionRepositorio.listarNoLeidasPorUsuario(usuarioId);
            if (!pendientes.isEmpty()) {
                emisor.send(SseEmitter.event()
                        .name("pendientes")
                        .data(pendientes.size() + " notificaciones pendientes"));
            }
        } catch (IOException e) {
            emisoresActivos.remove(usuarioId);
        }

        log.info("SSE: usuario {} suscrito. Emisores activos: {}",
                usuarioId, emisoresActivos.size());
        return emisor;
    }

    @Transactional
    public void enviar(TipoNotificacion tipo, String titulo, String mensaje,
                       Usuario destinatario, String entidadTipo, Long entidadId) {

        Notificacion notificacion = Notificacion.builder()
                .tipo(tipo)
                .titulo(titulo)
                .mensaje(mensaje)
                .destinatario(destinatario)
                .entidadTipo(entidadTipo)
                .entidadId(entidadId)
                .build();

        notificacionRepositorio.save(notificacion);

        // Enviar por SSE si el usuario está conectado
        if (destinatario != null) {
            enviarPorSSE(destinatario.getId(), titulo, mensaje);
        } else {
            // Notificación global — enviar a todos los conectados
            emisoresActivos.keySet().forEach(uid ->
                    enviarPorSSE(uid, titulo, mensaje));
        }
    }

    private void enviarPorSSE(Long usuarioId, String titulo, String mensaje) {
        SseEmitter emisor = emisoresActivos.get(usuarioId);
        if (emisor != null) {
            try {
                emisor.send(SseEmitter.event()
                        .name("notificacion")
                        .data(titulo + ": " + mensaje));
            } catch (IOException e) {
                emisoresActivos.remove(usuarioId);
                log.warn("SSE: emisor removido para usuario {} (conexión cerrada)", usuarioId);
            }
        }
    }

    // ── Historial y gestión ──────────────────

    @Transactional(readOnly = true)
    public List<NotificacionRespuestaDTO> noLeidas(Long usuarioId) {
        return notificacionRepositorio
                .listarNoLeidasPorUsuario(usuarioId)
                .stream().map(this::aDTO).toList();
    }

    @Transactional(readOnly = true)
    public Page<NotificacionRespuestaDTO> historial(Long usuarioId, Pageable pageable) {
        return notificacionRepositorio
                .historialPorUsuario(usuarioId, pageable)
                .map(this::aDTO);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepositorio.contarNoLeidas(usuarioId);
    }

    @Transactional
    public void marcarTodasLeidas(Long usuarioId) {
        notificacionRepositorio.marcarTodasLeidas(usuarioId);
    }

    @Transactional
    public void marcarLeida(Long notificacionId) {
        notificacionRepositorio.findById(notificacionId).ifPresent(n -> {
            n.setLeida(true);
            notificacionRepositorio.save(n);
        });
    }

    private NotificacionRespuestaDTO aDTO(Notificacion n) {
        return new NotificacionRespuestaDTO(
                n.getId(), n.getTipo(), n.getTitulo(), n.getMensaje(),
                n.isLeida(), n.getEntidadTipo(), n.getEntidadId(), n.getCreadoEn());
    }
}