package com.sgi.auto.taller;

import com.sgi.auto.clientes.ClienteRepositorio;
import com.sgi.auto.compartido.ReglaNegocioExcepcion;
import com.sgi.auto.inventario.MovimientoStockRepositorio;
import com.sgi.auto.inventario.Producto;
import com.sgi.auto.inventario.ProductoRepositorio;
import com.sgi.auto.taller.dto.*;
import com.sgi.auto.usuarios.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TallerServicio — pruebas unitarias")
class TallerServicioPrueba {

    @Mock OrdenDeTrabajoRepositorio otRepositorio;
    @Mock ProductoRepositorio productoRepositorio;
    @Mock MovimientoStockRepositorio movimientoStockRepositorio;
    @Mock ClienteRepositorio clienteRepositorio;
    @Mock UsuarioRepositorio usuarioRepositorio;

    @InjectMocks TallerServicio tallerServicio;

    private OrdenDeTrabajo otPrueba;
    private Producto productoPrueba;

    @BeforeEach
    void configurar() {
        otPrueba = new OrdenDeTrabajo();
        otPrueba.setId(1L);
        otPrueba.setPlaca("ABC123");
        otPrueba.setNombreCliente("Pedro Pérez");
        otPrueba.setDescripcionProblema("No enciende");
        otPrueba.setEstado(EstadoOT.RECIBIDO);
        otPrueba.setServicios(new ArrayList<>());
        otPrueba.setRepuestos(new ArrayList<>());
        otPrueba.setFotos(new ArrayList<>());
        otPrueba.setTotalServiciosCop(BigDecimal.ZERO);
        otPrueba.setTotalRepuestosCop(BigDecimal.ZERO);
        otPrueba.setDescuentoCop(BigDecimal.ZERO);
        otPrueba.setGranTotalCop(BigDecimal.ZERO);

        productoPrueba = new Producto();
        productoPrueba.setId(1L);
        productoPrueba.setNombre("Batería 12V");
        productoPrueba.setCodigo("BAT-001");
        productoPrueba.setStockActual(5);
    }

    @Test
    @DisplayName("Crear OT exitosamente")
    void crear_otValida_seGuarda() {
        OTCrearDTO solicitud = new OTCrearDTO(
                null, "Pedro Pérez", "300 111 2222",
                "ABC123", "Toyota", "Corolla", 2018,
                "Rojo", 80000, "No enciende",
                null, null, BigDecimal.ZERO);

        when(otRepositorio.save(any())).thenReturn(otPrueba);

        OTRespuestaDTO resultado = tallerServicio.crear(solicitud);

        assertThat(resultado.placa()).isEqualTo("ABC123");
        assertThat(resultado.estado()).isEqualTo(EstadoOT.RECIBIDO);
        verify(otRepositorio).save(any(OrdenDeTrabajo.class));
    }

    @Test
    @DisplayName("Agregar repuesto descuenta stock e ingresa al Kardex")
    void agregarRepuesto_stockSuficiente_descuentaStock() {
        when(otRepositorio.findById(1L)).thenReturn(Optional.of(otPrueba));
        when(productoRepositorio.findById(1L)).thenReturn(Optional.of(productoPrueba));
        when(productoRepositorio.save(any())).thenReturn(productoPrueba);
        when(movimientoStockRepositorio.save(any())).thenReturn(new com.sgi.auto.inventario.MovimientoStock());
        when(otRepositorio.save(any())).thenReturn(otPrueba);

        RepuestoOTDTO solicitud = new RepuestoOTDTO(1L, 2, new BigDecimal("80000"));
        tallerServicio.agregarRepuesto(1L, solicitud);

        assertThat(productoPrueba.getStockActual()).isEqualTo(3);
        verify(movimientoStockRepositorio).save(any());
    }

    @Test
    @DisplayName("Agregar repuesto con stock insuficiente lanza ReglaNegocioExcepcion")
    void agregarRepuesto_stockInsuficiente_lanzaReglaNegocio() {
        productoPrueba.setStockActual(1);

        when(otRepositorio.findById(1L)).thenReturn(Optional.of(otPrueba));
        when(productoRepositorio.findById(1L)).thenReturn(Optional.of(productoPrueba));

        RepuestoOTDTO solicitud = new RepuestoOTDTO(1L, 3, new BigDecimal("80000"));

        assertThatThrownBy(() -> tallerServicio.agregarRepuesto(1L, solicitud))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .hasMessageContaining("Stock insuficiente");

        verify(productoRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Cambiar estado a ENTREGADO registra fecha de entrega real")
    void cambiarEstado_aEntregado_registraFechaEntrega() {
        when(otRepositorio.findById(1L)).thenReturn(Optional.of(otPrueba));
        when(otRepositorio.save(any())).thenReturn(otPrueba);

        tallerServicio.cambiarEstado(1L,
                new CambioEstadoDTO(EstadoOT.ENTREGADO, "Vehículo entregado sin novedad"));

        assertThat(otPrueba.getEstado()).isEqualTo(EstadoOT.ENTREGADO);
        assertThat(otPrueba.getFechaEntregaReal()).isNotNull();
    }

    @Test
    @DisplayName("Modificar OT entregada lanza ReglaNegocioExcepcion")
    void agregarServicio_otEntregada_lanzaReglaNegocio() {
        otPrueba.setEstado(EstadoOT.ENTREGADO);

        when(otRepositorio.findById(1L)).thenReturn(Optional.of(otPrueba));

        ServicioOTDTO solicitud = new ServicioOTDTO(
                "Cambio de aceite", new BigDecimal("50000"), 1);

        assertThatThrownBy(() -> tallerServicio.agregarServicio(1L, solicitud))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .hasMessageContaining("ENTREGADO");
    }

    @Test
    @DisplayName("Agregar servicio recalcula totales correctamente")
    void agregarServicio_otActiva_recalculaTotales() {
        when(otRepositorio.findById(1L)).thenReturn(Optional.of(otPrueba));
        when(otRepositorio.save(any())).thenReturn(otPrueba);

        ServicioOTDTO solicitud = new ServicioOTDTO(
                "Diagnóstico electrónico", new BigDecimal("80000"), 1);

        tallerServicio.agregarServicio(1L, solicitud);

        assertThat(otPrueba.getTotalServiciosCop())
                .isEqualByComparingTo("80000");
        assertThat(otPrueba.getGranTotalCop())
                .isEqualByComparingTo("80000");
    }
}