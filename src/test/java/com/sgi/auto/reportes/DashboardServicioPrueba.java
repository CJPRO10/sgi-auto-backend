package com.sgi.auto.reportes;

import com.sgi.auto.caja.SesionCaja;
import com.sgi.auto.caja.SesionCajaRepositorio;
import com.sgi.auto.clientes.Credito;
import com.sgi.auto.clientes.CreditoRepositorio;
import com.sgi.auto.inventario.Producto;
import com.sgi.auto.inventario.ProductoRepositorio;
import com.sgi.auto.pos.EstadoVenta;
import com.sgi.auto.pos.MetodoPago;
import com.sgi.auto.pos.Venta;
import com.sgi.auto.pos.VentaRepositorio;
import com.sgi.auto.reportes.dto.DashboardDTO;
import com.sgi.auto.taller.EstadoOT;
import com.sgi.auto.taller.OrdenDeTrabajo;
import com.sgi.auto.taller.OrdenDeTrabajoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServicio — pruebas unitarias")
class DashboardServicioPrueba {

    @Mock VentaRepositorio ventaRepositorio;
    @Mock ProductoRepositorio productoRepositorio;
    @Mock OrdenDeTrabajoRepositorio otRepositorio;
    @Mock CreditoRepositorio creditoRepositorio;
    @Mock SesionCajaRepositorio sesionCajaRepositorio;

    @InjectMocks DashboardServicio dashboardServicio;

    private Venta ventaPrueba;
    private Producto productoPrueba;
    private OrdenDeTrabajo otPrueba;
    private Credito creditoPrueba;

    @BeforeEach
    void configurar() {
        ventaPrueba = new Venta();
        ventaPrueba.setId(1L);
        ventaPrueba.setEstado(EstadoVenta.COMPLETADA);
        ventaPrueba.setMetodoPago(MetodoPago.EFECTIVO);
        ventaPrueba.setTotalCop(new BigDecimal("130000"));
        ventaPrueba.setPuntosGanados(130);
        ventaPrueba.setCreadoEn(OffsetDateTime.now());
        ventaPrueba.setItems(new ArrayList<>());

        productoPrueba = new Producto();
        productoPrueba.setId(1L);
        productoPrueba.setNombre("Alternador 12V");
        productoPrueba.setStockActual(10);
        productoPrueba.setStockMinimo(3);
        productoPrueba.setEstaActivo(true);
        productoPrueba.setPrecioCompraConIva(new BigDecimal("50000"));

        otPrueba = new OrdenDeTrabajo();
        otPrueba.setId(1L);
        otPrueba.setEstado(EstadoOT.EN_REPARACION);
        otPrueba.setServicios(new ArrayList<>());
        otPrueba.setRepuestos(new ArrayList<>());
        otPrueba.setFotos(new ArrayList<>());

        creditoPrueba = Credito.builder()
                .cliente(null)
                .montoTotalCop(new BigDecimal("500000"))
                .montoPagadoCop(new BigDecimal("200000"))
                .estaActivo(true)
                .pagos(new ArrayList<>())
                .build();
        creditoPrueba.setId(1L);
    }

    @Test
    @DisplayName("Dashboard retorna resumen del día correctamente")
    void obtenerDashboard_conDatos_retornaResumenDia() {
        when(ventaRepositorio.findAll()).thenReturn(List.of(ventaPrueba));
        when(productoRepositorio.findAll()).thenReturn(List.of(productoPrueba));
        when(otRepositorio.findAll()).thenReturn(List.of(otPrueba));
        when(creditoRepositorio.findAll()).thenReturn(List.of(creditoPrueba));
        when(sesionCajaRepositorio.buscarSesionAbierta()).thenReturn(Optional.empty());

        DashboardDTO resultado = dashboardServicio.obtenerDashboard();

        assertThat(resultado).isNotNull();
        assertThat(resultado.resumenDia().totalVentas()).isEqualTo(1);
        assertThat(resultado.resumenDia().ingresosCop())
                .isEqualByComparingTo("130000");
    }

    @Test
    @DisplayName("Indicadores de inventario detectan stock bajo")
    void obtenerDashboard_productosStockBajo_losDetecta() {
        productoPrueba.setStockActual(2);
        productoPrueba.setStockMinimo(5);

        when(ventaRepositorio.findAll()).thenReturn(List.of());
        when(productoRepositorio.findAll()).thenReturn(List.of(productoPrueba));
        when(otRepositorio.findAll()).thenReturn(List.of());
        when(creditoRepositorio.findAll()).thenReturn(List.of());
        when(sesionCajaRepositorio.buscarSesionAbierta()).thenReturn(Optional.empty());

        DashboardDTO resultado = dashboardServicio.obtenerDashboard();

        assertThat(resultado.inventario().productosStockBajo()).isEqualTo(1);
        assertThat(resultado.inventario().productosAgotados()).isEqualTo(0);
    }

    @Test
    @DisplayName("Estado del taller cuenta OTs activas por estado")
    void obtenerDashboard_otsActivas_lasClasificaCorrectamente() {
        when(ventaRepositorio.findAll()).thenReturn(List.of());
        when(productoRepositorio.findAll()).thenReturn(List.of());
        when(otRepositorio.findAll()).thenReturn(List.of(otPrueba));
        when(creditoRepositorio.findAll()).thenReturn(List.of());
        when(sesionCajaRepositorio.buscarSesionAbierta()).thenReturn(Optional.empty());

        DashboardDTO resultado = dashboardServicio.obtenerDashboard();

        assertThat(resultado.taller().otsTotalesActivas()).isEqualTo(1);
        assertThat(resultado.taller().otsEnReparacion()).isEqualTo(1);
        assertThat(resultado.taller().otsListas()).isEqualTo(0);
    }

    @Test
    @DisplayName("Cartera de créditos calcula montos correctamente")
    void obtenerDashboard_conCreditos_calculaCarteraCorrectamente() {
        when(ventaRepositorio.findAll()).thenReturn(List.of());
        when(productoRepositorio.findAll()).thenReturn(List.of());
        when(otRepositorio.findAll()).thenReturn(List.of());
        when(creditoRepositorio.findAll()).thenReturn(List.of(creditoPrueba));
        when(sesionCajaRepositorio.buscarSesionAbierta()).thenReturn(Optional.empty());

        DashboardDTO resultado = dashboardServicio.obtenerDashboard();

        assertThat(resultado.cartera().totalCreditosActivos()).isEqualTo(1);
        assertThat(resultado.cartera().totalDeudaCop())
                .isEqualByComparingTo("500000");
        assertThat(resultado.cartera().totalRestanteCop())
                .isEqualByComparingTo("300000");
    }

    @Test
    @DisplayName("Resumen de caja refleja sesión abierta")
    void obtenerDashboard_conSesionAbierta_reflejaResumenCaja() {
        SesionCaja sesion = SesionCaja.builder()
                .saldoInicialCop(new BigDecimal("200000"))
                .totalVentasCop(new BigDecimal("500000"))
                .totalGastosCop(new BigDecimal("50000"))
                .totalAbonosCreditoCop(BigDecimal.ZERO)
                .build();

        when(ventaRepositorio.findAll()).thenReturn(List.of());
        when(productoRepositorio.findAll()).thenReturn(List.of());
        when(otRepositorio.findAll()).thenReturn(List.of());
        when(creditoRepositorio.findAll()).thenReturn(List.of());
        when(sesionCajaRepositorio.buscarSesionAbierta())
                .thenReturn(Optional.of(sesion));

        DashboardDTO resultado = dashboardServicio.obtenerDashboard();

        assertThat(resultado.caja().cajaAbierta()).isTrue();
        assertThat(resultado.caja().saldoEsperadoCop())
                .isEqualByComparingTo("650000");
    }

    @Test
    @DisplayName("Sin sesión de caja abierta retorna caja cerrada")
    void obtenerDashboard_sinSesionAbierta_retornaCajaCerrada() {
        when(ventaRepositorio.findAll()).thenReturn(List.of());
        when(productoRepositorio.findAll()).thenReturn(List.of());
        when(otRepositorio.findAll()).thenReturn(List.of());
        when(creditoRepositorio.findAll()).thenReturn(List.of());
        when(sesionCajaRepositorio.buscarSesionAbierta())
                .thenReturn(Optional.empty());

        DashboardDTO resultado = dashboardServicio.obtenerDashboard();

        assertThat(resultado.caja().cajaAbierta()).isFalse();
        assertThat(resultado.caja().saldoEsperadoCop())
                .isEqualByComparingTo("0");
    }
}