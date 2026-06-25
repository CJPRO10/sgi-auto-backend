package com.sgi.auto.caja;

import com.sgi.auto.caja.dto.*;
import com.sgi.auto.compartido.ReglaNegocioExcepcion;
import com.sgi.auto.usuarios.RolUsuario;
import com.sgi.auto.usuarios.Usuario;
import com.sgi.auto.usuarios.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CajaServicio — pruebas unitarias")
class CajaServicioPrueba {

    @Mock SesionCajaRepositorio sesionCajaRepositorio;
    @Mock MovimientoCajaRepositorio movimientoCajaRepositorio;
    @Mock UsuarioRepositorio usuarioRepositorio;

    @InjectMocks CajaServicio cajaServicio;

    private Usuario cajera;
    private SesionCaja sesionAbierta;

    @BeforeEach
    void configurar() {
        cajera = Usuario.builder()
                .nombreCompleto("María López")
                .nombreUsuario("maria.lopez")
                .rol(RolUsuario.CAJERA)
                .build();
        cajera.setId(1L);

        sesionAbierta = SesionCaja.builder()
                .cajera(cajera)
                .saldoInicialCop(new BigDecimal("200000"))
                .totalVentasCop(new BigDecimal("500000"))
                .totalGastosCop(new BigDecimal("50000"))
                .totalAbonosCreditoCop(BigDecimal.ZERO)
                .build();
        sesionAbierta.setId(1L);

        // Simular usuario autenticado en el SecurityContext
        var auth = new UsernamePasswordAuthenticationToken(
                "maria.lopez", null,
                List.of(new SimpleGrantedAuthority("ROLE_CAJERA")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Abrir sesión de caja exitosamente")
    void abrirSesion_sinSesionActiva_creaCorrectamente() {
        when(sesionCajaRepositorio.buscarSesionAbierta())
                .thenReturn(Optional.empty());
        when(usuarioRepositorio.buscarPorNombreUsuario("maria.lopez"))
                .thenReturn(Optional.of(cajera));
        when(sesionCajaRepositorio.save(any())).thenReturn(sesionAbierta);
        when(movimientoCajaRepositorio.save(any())).thenReturn(new MovimientoCaja());
        when(movimientoCajaRepositorio.listarPorSesion(any())).thenReturn(List.of());

        SesionCajaRespuestaDTO resultado = cajaServicio.abrirSesion(
                new AperturaCajaDTO(new BigDecimal("200000")));

        assertThat(resultado).isNotNull();
        verify(sesionCajaRepositorio).save(any(SesionCaja.class));
        verify(movimientoCajaRepositorio).save(any(MovimientoCaja.class));
    }

    @Test
    @DisplayName("Abrir sesión cuando ya hay una abierta lanza ReglaNegocioExcepcion")
    void abrirSesion_conSesionActiva_lanzaReglaNegocio() {
        when(sesionCajaRepositorio.buscarSesionAbierta())
                .thenReturn(Optional.of(sesionAbierta));

        assertThatThrownBy(() -> cajaServicio.abrirSesion(
                new AperturaCajaDTO(new BigDecimal("200000"))))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .hasMessageContaining("Ya existe una sesión");
    }

    @Test
    @DisplayName("Cerrar sesión calcula diferencia correctamente")
    void cerrarSesion_conSesionAbierta_calculaDiferenciaCorrectamente() {
        when(sesionCajaRepositorio.buscarSesionAbierta())
                .thenReturn(Optional.of(sesionAbierta));
        when(sesionCajaRepositorio.save(any())).thenReturn(sesionAbierta);
        when(movimientoCajaRepositorio.listarPorSesion(any())).thenReturn(List.of());

        // Saldo esperado = 200000 + 500000 - 50000 = 650000
        // Saldo contado = 640000 → diferencia = -10000
        CierreCajaDTO cierre = new CierreCajaDTO(
                new BigDecimal("640000"), "Cierre del día");

        cajaServicio.cerrarSesion(cierre);

        assertThat(sesionAbierta.isEstaAbierta()).isFalse();
        assertThat(sesionAbierta.getSaldoFinalCop())
                .isEqualByComparingTo("640000");
        assertThat(sesionAbierta.getDiferenciaCop())
                .isEqualByComparingTo("-10000");
    }

    @Test
    @DisplayName("Cerrar sesión sin sesión abierta lanza ReglaNegocioExcepcion")
    void cerrarSesion_sinSesionAbierta_lanzaReglaNegocio() {
        when(sesionCajaRepositorio.buscarSesionAbierta())
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cajaServicio.cerrarSesion(
                new CierreCajaDTO(new BigDecimal("500000"), null)))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .hasMessageContaining("No hay ninguna sesión");
    }

    @Test
    @DisplayName("Registrar gasto actualiza total gastos de la sesión")
    void registrarGasto_conSesionAbierta_actualizaTotalGastos() {
        when(sesionCajaRepositorio.buscarSesionAbierta())
                .thenReturn(Optional.of(sesionAbierta));
        when(movimientoCajaRepositorio.save(any())).thenReturn(new MovimientoCaja());
        when(sesionCajaRepositorio.save(any())).thenReturn(sesionAbierta);

        cajaServicio.registrarGasto(
                new GastoDTO(new BigDecimal("30000"), "Papelería"));

        assertThat(sesionAbierta.getTotalGastosCop())
                .isEqualByComparingTo("80000"); // 50000 + 30000
        verify(movimientoCajaRepositorio).save(any(MovimientoCaja.class));
    }
}