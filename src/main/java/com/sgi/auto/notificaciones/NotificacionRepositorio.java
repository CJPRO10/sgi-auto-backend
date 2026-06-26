package com.sgi.auto.notificaciones;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacionRepositorio extends JpaRepository<Notificacion, Long> {

    // Notificaciones no leídas para un usuario o globales
    @Query("""
            SELECT n FROM Notificacion n
            WHERE n.leida = false
              AND (n.destinatario IS NULL OR n.destinatario.id = :usuarioId)
            ORDER BY n.creadoEn DESC
            """)
    List<Notificacion> listarNoLeidasPorUsuario(@Param("usuarioId") Long usuarioId);

    // Historial completo
    @Query("""
            SELECT n FROM Notificacion n
            WHERE n.destinatario IS NULL OR n.destinatario.id = :usuarioId
            ORDER BY n.creadoEn DESC
            """)
    Page<Notificacion> historialPorUsuario(@Param("usuarioId") Long usuarioId,
                                           Pageable pageable);

    // Marcar todas como leídas
    @Modifying
    @Query("""
            UPDATE Notificacion n SET n.leida = true
            WHERE n.leida = false
              AND (n.destinatario IS NULL OR n.destinatario.id = :usuarioId)
            """)
    void marcarTodasLeidas(@Param("usuarioId") Long usuarioId);

    // Contar no leídas (para el badge)
    @Query("""
            SELECT COUNT(n) FROM Notificacion n
            WHERE n.leida = false
              AND (n.destinatario IS NULL OR n.destinatario.id = :usuarioId)
            """)
    long contarNoLeidas(@Param("usuarioId") Long usuarioId);
}