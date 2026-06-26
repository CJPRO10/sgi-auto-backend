package com.sgi.auto.reportes;

import com.sgi.auto.clientes.CreditoRepositorio;
import com.sgi.auto.inventario.MovimientoStockRepositorio;
import com.sgi.auto.inventario.ProductoRepositorio;
import com.sgi.auto.pos.VentaRepositorio;
import com.sgi.auto.reportes.dto.*;
import com.sgi.auto.taller.OrdenDeTrabajoRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

// Servicio de reportes y exportaciones.
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteServicio {

    private final VentaRepositorio ventaRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final OrdenDeTrabajoRepositorio otRepositorio;
    private final CreditoRepositorio creditoRepositorio;
    private final MovimientoStockRepositorio movimientoStockRepositorio;

    // ── Reporte de Ventas ────────────────────────────

    @Transactional(readOnly = true)
    public List<VentaReporteDTO> reporteVentas(LocalDate desde, LocalDate hasta) {
        OffsetDateTime inicio = desde.atStartOfDay().atOffset(ZoneOffset.of("-05:00"));
        OffsetDateTime fin = hasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.of("-05:00"));

        return ventaRepositorio.findAll().stream()
                .filter(v -> v.getCreadoEn() != null
                        && !v.getCreadoEn().isBefore(inicio)
                        && v.getCreadoEn().isBefore(fin)
                        && v.getEstado().name().equals("COMPLETADA"))
                .map(v -> new VentaReporteDTO(
                        v.getId(),
                        v.getCliente() != null
                                ? v.getCliente().getNombreCompleto()
                                : v.getNombreClienteAnonimo(),
                        v.getMetodoPago().name(),
                        v.getTotalCop(),
                        v.getItems().size(),
                        v.getCreadoEn()))
                .toList();
    }

    // ── Reporte de Inventario ────────────────────────

    @Transactional(readOnly = true)
    public List<ProductoReporteDTO> reporteInventario() {
        return productoRepositorio.findAll().stream()
                .filter(p -> p.getEliminadoEn() == null && p.isEstaActivo())
                .map(p -> new ProductoReporteDTO(
                        p.getCodigo(), p.getNombre(),
                        p.getCategoria() != null ? p.getCategoria().getNombre() : "Sin categoría",
                        p.getStockActual(), p.getStockMinimo(),
                        p.getPrecioVentaDetal(), p.getPrecioVentaMayor(),
                        p.getMargenGananciaPct(),
                        p.getStockActual() <= p.getStockMinimo()))
                .toList();
    }

    // ── Reporte de OTs / Taller ─────────────────────

    @Transactional(readOnly = true)
    public List<OTReporteDTO> reporteTaller(LocalDate desde, LocalDate hasta) {
        OffsetDateTime inicio = desde.atStartOfDay().atOffset(ZoneOffset.of("-05:00"));
        OffsetDateTime fin = hasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.of("-05:00"));

        return otRepositorio.findAll().stream()
                .filter(o -> o.getCreadoEn() != null
                        && !o.getCreadoEn().isBefore(inicio)
                        && o.getCreadoEn().isBefore(fin))
                .map(o -> new OTReporteDTO(
                        o.getId(), o.getPlaca(), o.getNombreCliente(),
                        o.getMecanico() != null ? o.getMecanico().getNombreCompleto() : "Sin asignar",
                        o.getEstado().name(),
                        o.getGranTotalCop(),
                        o.getCreadoEn(), o.getFechaEntregaReal()))
                .toList();
    }

    // ── Exportar Excel ───────────────────────────────

    public byte[] exportarVentasExcel(List<VentaReporteDTO> ventas) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet hoja = libro.createSheet("Ventas");

            // Estilo de encabezado
            CellStyle estiloEncabezado = libro.createCellStyle();
            Font fuenteEncabezado = libro.createFont();
            fuenteEncabezado.setBold(true);
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Fila de encabezados
            Row encabezado = hoja.createRow(0);
            String[] columnas = {"ID", "Cliente", "Método Pago", "Total (COP)", "Items", "Fecha"};
            for (int i = 0; i < columnas.length; i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(columnas[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            // Filas de datos
            int fila = 1;
            for (VentaReporteDTO venta : ventas) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(venta.id());
                row.createCell(1).setCellValue(venta.nombreCliente());
                row.createCell(2).setCellValue(venta.metodoPago());
                row.createCell(3).setCellValue(venta.totalCop().doubleValue());
                row.createCell(4).setCellValue(venta.cantidadItems());
                row.createCell(5).setCellValue(
                        venta.fecha() != null ? venta.fecha().toString() : "");
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < columnas.length; i++) {
                hoja.autoSizeColumn(i);
            }

            libro.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportarInventarioExcel(List<ProductoReporteDTO> productos) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet hoja = libro.createSheet("Inventario");

            CellStyle estiloEncabezado = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setBold(true);
            estiloEncabezado.setFont(fuente);
            estiloEncabezado.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle estiloStockBajo = libro.createCellStyle();
            estiloStockBajo.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            estiloStockBajo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row encabezado = hoja.createRow(0);
            String[] columnas = {"Código", "Nombre", "Categoría", "Stock Actual",
                    "Stock Mínimo", "Precio Detal", "Precio Mayor", "Margen %", "Alerta"};
            for (int i = 0; i < columnas.length; i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(columnas[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            int fila = 1;
            for (ProductoReporteDTO p : productos) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(p.codigo());
                row.createCell(1).setCellValue(p.nombre());
                row.createCell(2).setCellValue(p.categoria());
                row.createCell(3).setCellValue(p.stockActual());
                row.createCell(4).setCellValue(p.stockMinimo());
                row.createCell(5).setCellValue(p.precioVentaDetal().doubleValue());
                row.createCell(6).setCellValue(p.precioVentaMayor().doubleValue());
                row.createCell(7).setCellValue(
                        p.margenGananciaPct() != null ? p.margenGananciaPct().doubleValue() : 0);

                Cell celdaAlerta = row.createCell(8);
                if (p.stockBajo()) {
                    celdaAlerta.setCellValue("⚠ STOCK BAJO");
                    celdaAlerta.setCellStyle(estiloStockBajo);
                } else {
                    celdaAlerta.setCellValue("OK");
                }
            }

            for (int i = 0; i < columnas.length; i++) {
                hoja.autoSizeColumn(i);
            }

            libro.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportarTallerExcel(List<OTReporteDTO> ots) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet hoja = libro.createSheet("Órdenes de Trabajo");

            CellStyle estiloEncabezado = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setBold(true);
            estiloEncabezado.setFont(fuente);
            estiloEncabezado.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row encabezado = hoja.createRow(0);
            String[] columnas = {"ID", "Placa", "Cliente", "Mecánico",
                    "Estado", "Total (COP)", "Ingreso", "Entrega"};
            for (int i = 0; i < columnas.length; i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(columnas[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            int fila = 1;
            for (OTReporteDTO ot : ots) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(ot.id());
                row.createCell(1).setCellValue(ot.placa());
                row.createCell(2).setCellValue(ot.nombreCliente());
                row.createCell(3).setCellValue(ot.mecanicoNombre());
                row.createCell(4).setCellValue(ot.estado());
                row.createCell(5).setCellValue(ot.granTotalCop().doubleValue());
                row.createCell(6).setCellValue(
                        ot.fechaIngreso() != null ? ot.fechaIngreso().toString() : "");
                row.createCell(7).setCellValue(
                        ot.fechaEntrega() != null ? ot.fechaEntrega().toString() : "");
            }

            for (int i = 0; i < columnas.length; i++) {
                hoja.autoSizeColumn(i);
            }

            libro.write(out);
            return out.toByteArray();
        }
    }

    // ── Lista de precios ─────────────────────

    @Transactional(readOnly = true)
    public List<ProductoReporteDTO> listaPrecios(Long categoriaId) {
        return productoRepositorio.listarParaListaPrecios().stream()
                .filter(p -> categoriaId == null
                        || (p.getCategoria() != null
                        && p.getCategoria().getId().equals(categoriaId)))
                .map(p -> new ProductoReporteDTO(
                        p.getCodigo(), p.getNombre(),
                        p.getCategoria() != null ? p.getCategoria().getNombre() : "Sin categoría",
                        p.getStockActual(), p.getStockMinimo(),
                        p.getPrecioVentaDetal(), p.getPrecioVentaMayor(),
                        p.getMargenGananciaPct(), false))
                .toList();
    }
}