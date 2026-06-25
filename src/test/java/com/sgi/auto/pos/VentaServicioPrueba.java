package com.sgi.auto.pos;

import com.sgi.auto.clientes.Cliente;
import com.sgi.auto.clientes.ClienteRepositorio;
import com.sgi.auto.compartido.ReglaNegocioExcepcion;
import com.sgi.auto.inventario.MovimientoStockRepositorio;
import com.sgi.auto.inventario.Producto;
import com.sgi.auto.inventario.ProductoRepositorio;
import com.sgi.auto.pos.dto.AnulacionDTO;
import com.sgi.auto.pos.dto.VentaCrearDTO;
import com.sgi.auto.pos.dto.VentaRespuestaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaServicio — pruebas unitarias")
class VentaServicioPrueba {

    @Mock VentaRepositorio ventaRepositorio;
    @Mock ProductoRepositorio productoRepositorio;
    @Mock MovimientoStockRepositorio movimientoStockRepositorio;
    @Mock ClienteRepositorio clienteRepositorio;

    @InjectMocks VentaServicio ventaServicio;

    private Producto productoPrueba;
    private VentaCrearDTO solicitudVenta;
    private String claveIdempotencia;

    @BeforeEach
    void configurar() {
        claveIdempotencia = UUID.randomUUID().toString();

        productoPrueba = new Producto();
        productoPrueba.setId(1L);
        productoPrueba.setNombre("Alternador 12V");
        productoPrueba.setCodigo("ALT-001");
        productoPrueba.setStockActual(10);
        productoPrueba.setPrecioVentaDetal(new BigDecimal("65000"));

        solicitudVenta = new VentaCrearDTO(
                claveIdempotencia,
                null,
                "Cliente general",
                MetodoPago.EFECTIVO,
                List.of(new VentaCrearDTO.ItemVentaDTO(
                        1L, 2,
                        new BigDecimal("65000"),
                        BigDecimal.ZERO)),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("150000"));
    }

    @Test
    @DisplayName("Crear venta exitosamente descuenta stock y registra movimiento")
    void crear_ventaValida_descuentaStockYRegistraMovimiento() {
        when(ventaRepositorio.findByClaveIdempotencia(claveIdempotencia))
                .thenReturn(Optional.empty());
        when(productoRepositorio.findById(1L))
                .thenReturn(Optional.of(productoPrueba));
        when(productoRepositorio.save(any())).thenReturn(productoPrueba);
        when(movimientoStockRepositorio.save(any())).thenReturn(new com.sgi.auto.inventario.MovimientoStock());

        Venta ventaGuardada = new Venta();
        ventaGuardada.setId(1L);
        ventaGuardada.setClaveIdempotencia(claveIdempotencia);
        ventaGuardada.setEstado(EstadoVenta.COMPLETADA);
        ventaGuardada.setMetodoPago(MetodoPago.EFECTIVO);
        ventaGuardada.setTotalCop(new BigDecimal("130000"));
        ventaGuardada.setSubtotalCop(new BigDecimal("130000"));
        ventaGuardada.setDescuentoCop(BigDecimal.ZERO);
        ventaGuardada.setMontoPagadoCop(new BigDecimal("150000"));
        ventaGuardada.setVueltoCop(new BigDecimal("20000"));
        ventaGuardada.setNombreClienteAnonimo("Cliente general");
        ventaGuardada.setItems(List.of());

        when(ventaRepositorio.save(any(Venta.class))).thenReturn(ventaGuardada);

        VentaRespuestaDTO resultado = ventaServicio.crear(solicitudVenta);

        assertThat(resultado.estado()).isEqualTo(EstadoVenta.COMPLETADA);
        assertThat(resultado.vueltoCop()).isEqualByComparingTo("20000");
        assertThat(productoPrueba.getStockActual()).isEqualTo(8);
        verify(movimientoStockRepositorio).save(any());
    }

    @Test
    @DisplayName("Venta con clave idempotencia duplicada retorna la venta existente")
    void crear_claveIdempotenciaDuplicada_retornaExistente() {
        Venta ventaExistente = new Venta();
        ventaExistente.setId(5L);
        ventaExistente.setClaveIdempotencia(claveIdempotencia);
        ventaExistente.setEstado(EstadoVenta.COMPLETADA);
        ventaExistente.setMetodoPago(MetodoPago.EFECTIVO);
        ventaExistente.setTotalCop(new BigDecimal("130000"));
        ventaExistente.setSubtotalCop(new BigDecimal("130000"));
        ventaExistente.setDescuentoCop(BigDecimal.ZERO);
        ventaExistente.setMontoPagadoCop(new BigDecimal("150000"));
        ventaExistente.setVueltoCop(new BigDecimal("20000"));
        ventaExistente.setNombreClienteAnonimo("Cliente general");
        ventaExistente.setItems(List.of());

        when(ventaRepositorio.findByClaveIdempotencia(claveIdempotencia))
                .thenReturn(Optional.of(ventaExistente));

        VentaRespuestaDTO resultado = ventaServicio.crear(solicitudVenta);

        assertThat(resultado.id()).isEqualTo(5L);
        verify(productoRepositorio, never()).findById(any());
        verify(ventaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Venta con stock insuficiente lanza ReglaNegocioExcepcion")
    void crear_stockInsuficiente_lanzaReglaNegocio() {
        productoPrueba.setStockActual(1); // solo 1 en stock, pide 2

        when(ventaRepositorio.findByClaveIdempotencia(claveIdempotencia))
                .thenReturn(Optional.empty());
        when(productoRepositorio.findById(1L))
                .thenReturn(Optional.of(productoPrueba));

        assertThatThrownBy(() -> ventaServicio.crear(solicitudVenta))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .hasMessageContaining("Stock insuficiente");

        verify(ventaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Anular venta revierte el stock")
    void anular_ventaCompletada_revierteStock() {
        ItemVenta item = new ItemVenta();
        item.setProducto(productoPrueba);
        item.setCantidad(2);
        item.setPrecioUnitarioCop(new BigDecimal("65000"));
        item.setSubtotalCop(new BigDecimal("130000"));
        item.setNombreProductoSnapshot("Alternador 12V");
        item.setCodigoProductoSnapshot("ALT-001");

        Venta venta = new Venta();
        venta.setId(1L);
        venta.setEstado(EstadoVenta.COMPLETADA);
        venta.setMetodoPago(MetodoPago.EFECTIVO);
        venta.setTotalCop(new BigDecimal("130000"));
        venta.setSubtotalCop(new BigDecimal("130000"));
        venta.setDescuentoCop(BigDecimal.ZERO);
        venta.setMontoPagadoCop(new BigDecimal("130000"));
        venta.setVueltoCop(BigDecimal.ZERO);
        venta.setItems(List.of(item));

        when(ventaRepositorio.findById(1L)).thenReturn(Optional.of(venta));
        when(productoRepositorio.save(any())).thenReturn(productoPrueba);
        when(movimientoStockRepositorio.save(any())).thenReturn(new com.sgi.auto.inventario.MovimientoStock());
        when(ventaRepositorio.save(any())).thenReturn(venta);

        ventaServicio.anular(1L, new AnulacionDTO("Error en la venta"));

        assertThat(productoPrueba.getStockActual()).isEqualTo(12); // 10 + 2 devueltos
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.ANULADA);
        verify(movimientoStockRepositorio).save(any());
    }

    @Test
    @DisplayName("Anular venta ya anulada lanza ReglaNegocioExcepcion")
    void anular_ventaYaAnulada_lanzaReglaNegocio() {
        Venta venta = new Venta();
        venta.setId(1L);
        venta.setEstado(EstadoVenta.ANULADA);

        when(ventaRepositorio.findById(1L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() ->
                ventaServicio.anular(1L, new AnulacionDTO("Intentar anular de nuevo")))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .hasMessageContaining("ya está anulada");
    }
}