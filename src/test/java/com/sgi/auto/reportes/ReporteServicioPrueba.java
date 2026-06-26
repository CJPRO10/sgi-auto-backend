package com.sgi.auto.reportes;

import com.sgi.auto.clientes.CreditoRepositorio;
import com.sgi.auto.inventario.MovimientoStockRepositorio;
import com.sgi.auto.inventario.Producto;
import com.sgi.auto.inventario.ProductoRepositorio;
import com.sgi.auto.pos.EstadoVenta;
import com.sgi.auto.pos.MetodoPago;
import com.sgi.auto.pos.Venta;
import com.sgi.auto.pos.VentaRepositorio;
import com.sgi.auto.reportes.dto.ProductoReporteDTO;
import com.sgi.auto.reportes.dto.VentaReporteDTO;
import com.sgi.auto.taller.OrdenDeTrabajoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReporteServicio — pruebas unitarias")
class ReporteServicioPrueba {

    @Mock VentaRepositorio ventaRepositorio;
    @Mock ProductoRepositorio productoRepositorio;
    @Mock OrdenDeTrabajoRepositorio otRepositorio;
    @Mock CreditoRepositorio creditoRepositorio;
    @Mock MovimientoStockRepositorio movimientoStockRepositorio;

    @InjectMocks ReporteServicio reporteServicio;

    private Venta ventaPrueba;
    private Producto productoPrueba;

    @BeforeEach
    void configurar() {
        ventaPrueba = new Venta();
        ventaPrueba.setId(1L);
        ventaPrueba.setEstado(EstadoVenta.COMPLETADA);
        ventaPrueba.setMetodoPago(MetodoPago.EFECTIVO);
        ventaPrueba.setTotalCop(new BigDecimal("130000"));
        ventaPrueba.setNombreClienteAnonimo("Cliente general");
        ventaPrueba.setCreadoEn(OffsetDateTime.now(ZoneOffset.of("-05:00")));
        ventaPrueba.setItems(new ArrayList<>());

        productoPrueba = new Producto();
        productoPrueba.setId(1L);
        productoPrueba.setCodigo("ALT-001");
        productoPrueba.setNombre("Alternador 12V");
        productoPrueba.setStockActual(10);
        productoPrueba.setStockMinimo(3);
        productoPrueba.setPrecioVentaDetal(new BigDecimal("65000"));
        productoPrueba.setPrecioVentaMayor(new BigDecimal("60000"));
        productoPrueba.setEstaActivo(true);
    }

    @Test
    @DisplayName("Reporte de ventas filtra por fecha correctamente")
    void reporteVentas_filtroPorFecha_retornaVentasEnRango() {
        when(ventaRepositorio.findAll()).thenReturn(List.of(ventaPrueba));

        List<VentaReporteDTO> resultado = reporteServicio.reporteVentas(
                LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).totalCop())
                .isEqualByComparingTo("130000");
    }

    @Test
    @DisplayName("Reporte de ventas excluye ventas anuladas")
    void reporteVentas_ventaAnulada_noSeIncluye() {
        ventaPrueba.setEstado(EstadoVenta.ANULADA);
        when(ventaRepositorio.findAll()).thenReturn(List.of(ventaPrueba));

        List<VentaReporteDTO> resultado = reporteServicio.reporteVentas(
                LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Reporte de inventario incluye todos los productos activos")
    void reporteInventario_productosActivos_retornaTodos() {
        when(productoRepositorio.findAll()).thenReturn(List.of(productoPrueba));

        List<ProductoReporteDTO> resultado = reporteServicio.reporteInventario();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).codigo()).isEqualTo("ALT-001");
        assertThat(resultado.get(0).stockBajo()).isFalse();
    }

    @Test
    @DisplayName("Reporte de inventario marca correctamente stock bajo mínimo")
    void reporteInventario_stockBajoMinimo_marcaAlerta() {
        productoPrueba.setStockActual(2);
        productoPrueba.setStockMinimo(5);
        when(productoRepositorio.findAll()).thenReturn(List.of(productoPrueba));

        List<ProductoReporteDTO> resultado = reporteServicio.reporteInventario();

        assertThat(resultado.get(0).stockBajo()).isTrue();
    }

    @Test
    @DisplayName("Exportar ventas a Excel genera bytes válidos")
    void exportarVentasExcel_conDatos_generaArchivoValido() throws IOException {
        List<VentaReporteDTO> ventas = List.of(
                new VentaReporteDTO(1L, "Juan García", "EFECTIVO",
                        new BigDecimal("130000"), 2,
                        OffsetDateTime.now()));

        byte[] resultado = reporteServicio.exportarVentasExcel(ventas);

        assertThat(resultado).isNotEmpty();
        // Los archivos XLSX empiezan con la firma PK (ZIP)
        assertThat(resultado[0]).isEqualTo((byte) 'P');
        assertThat(resultado[1]).isEqualTo((byte) 'K');
    }

    @Test
    @DisplayName("Exportar inventario a Excel genera bytes válidos")
    void exportarInventarioExcel_conDatos_generaArchivoValido() throws IOException {
        List<ProductoReporteDTO> productos = List.of(
                new ProductoReporteDTO("ALT-001", "Alternador 12V", "Eléctricos",
                        10, 3, new BigDecimal("65000"),
                        new BigDecimal("60000"), new BigDecimal("50.00"), false));

        byte[] resultado = reporteServicio.exportarInventarioExcel(productos);

        assertThat(resultado).isNotEmpty();
        assertThat(resultado[0]).isEqualTo((byte) 'P');
        assertThat(resultado[1]).isEqualTo((byte) 'K');
    }
}