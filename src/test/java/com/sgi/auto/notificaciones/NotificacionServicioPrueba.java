package com.sgi.auto.notificaciones;

import com.sgi.auto.notificaciones.dto.NotificacionRespuestaDTO;
import com.sgi.auto.usuarios.RolUsuario;
import com.sgi.auto.usuarios.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionServicio — pruebas unitarias")
class NotificacionServicioPrueba {

    @Mock NotificacionRepositorio notificacionRepositorio;

    @InjectMocks NotificacionServicio notificacionServicio;

    private Usuario usuarioPrueba;
    private Notificacion notificacionPrueba;

    @BeforeEach
    void configurar() {
        usuarioPrueba = Usuario.builder()
                .nombreCompleto("Admin")
                .nombreUsuario("admin")
                .rol(RolUsuario.DUENO)
                .build();
        usuarioPrueba.setId(1L);

        notificacionPrueba = Notificacion.builder()
                .tipo(TipoNotificacion.STOCK_BAJO)
                .titulo("Stock bajo")
                .mensaje("Alternador 12V está por debajo del mínimo")
                .leida(false)
                .entidadTipo("Producto")
                .entidadId(1L)
                .build();
        notificacionPrueba.setId(1L);
    }

    @Test
    @DisplayName("Suscribir genera un SseEmitter válido")
    void suscribir_usuarioValido_retornaEmitter() {
        when(notificacionRepositorio.listarNoLeidasPorUsuario(1L))
                .thenReturn(List.of());

        SseEmitter emisor = notificacionServicio.suscribir(1L);

        assertThat(emisor).isNotNull();
    }

    @Test
    @DisplayName("Enviar notificación la persiste en BD")
    void enviar_notificacionGlobal_seGuardaEnBD() {
        when(notificacionRepositorio.save(any())).thenReturn(notificacionPrueba);

        notificacionServicio.enviar(
                TipoNotificacion.STOCK_BAJO,
                "Stock bajo",
                "Alternador 12V bajo mínimo",
                null, "Producto", 1L);

        verify(notificacionRepositorio).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("Contar no leídas retorna cantidad correcta")
    void contarNoLeidas_conNotificaciones_retornaCantidad() {
        when(notificacionRepositorio.contarNoLeidas(1L)).thenReturn(3L);

        long resultado = notificacionServicio.contarNoLeidas(1L);

        assertThat(resultado).isEqualTo(3L);
    }

    @Test
    @DisplayName("Marcar todas leídas invoca el repositorio")
    void marcarTodasLeidas_usuarioValido_invocaRepositorio() {
        notificacionServicio.marcarTodasLeidas(1L);
        verify(notificacionRepositorio).marcarTodasLeidas(1L);
    }

    @Test
    @DisplayName("Historial retorna página de notificaciones")
    void historial_usuarioValido_retornaPagina() {
        notificacionPrueba.setCreadoEn(OffsetDateTime.now());
        when(notificacionRepositorio.historialPorUsuario(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(notificacionPrueba)));

        var resultado = notificacionServicio.historial(1L, PageRequest.of(0, 20));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).tipo())
                .isEqualTo(TipoNotificacion.STOCK_BAJO);
    }

    @Test
    @DisplayName("Marcar una notificación como leída la actualiza")
    void marcarLeida_notificacionExistente_laActualiza() {
        when(notificacionRepositorio.findById(1L))
                .thenReturn(Optional.of(notificacionPrueba));
        when(notificacionRepositorio.save(any())).thenReturn(notificacionPrueba);

        notificacionServicio.marcarLeida(1L);

        assertThat(notificacionPrueba.isLeida()).isTrue();
        verify(notificacionRepositorio).save(notificacionPrueba);
    }
}