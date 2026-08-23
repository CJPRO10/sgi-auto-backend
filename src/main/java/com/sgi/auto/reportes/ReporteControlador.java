package com.sgi.auto.reportes;

import com.sgi.auto.compartido.ApiRespuesta;
import com.sgi.auto.reportes.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DUENO')")
public class ReporteControlador {

    private final ReporteServicio reporteServicio;

    // ── Datos JSON ────────────────────────────────────────────

    @GetMapping("/ventas")
    public ResponseEntity<ApiRespuesta<List<VentaReporteDTO>>> ventas(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (desde == null) desde = LocalDate.now().minusMonths(1);
        if (hasta == null) hasta = LocalDate.now();
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                reporteServicio.reporteVentas(desde, hasta)));
    }

    @GetMapping("/inventario")
    public ResponseEntity<ApiRespuesta<List<ProductoReporteDTO>>> inventario() {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                reporteServicio.reporteInventario()));
    }

    @GetMapping("/productos-sin-movimiento")
    public ResponseEntity<ApiRespuesta<List<ProductoSinMovimientoDTO>>> productosSinMovimiento(
            @RequestParam(required = false, defaultValue = "30") int dias) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                reporteServicio.productosSinMovimiento(dias)));
    }

    @GetMapping("/taller")
    public ResponseEntity<ApiRespuesta<List<OTReporteDTO>>> taller(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (desde == null) desde = LocalDate.now().minusMonths(1);
        if (hasta == null) hasta = LocalDate.now();
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                reporteServicio.reporteTaller(desde, hasta)));
    }

    @GetMapping("/lista-precios")
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<ApiRespuesta<List<ProductoReporteDTO>>> listaPrecios(
            @RequestParam(required = false) Long categoriaId) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                reporteServicio.listaPrecios(categoriaId)));
    }

    // ── Exportar Excel ────────────────────────────────────────

    @GetMapping("/ventas/excel")
    public ResponseEntity<byte[]> ventasExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta)
            throws IOException {
        if (desde == null) desde = LocalDate.now().minusMonths(1);
        if (hasta == null) hasta = LocalDate.now();

        List<VentaReporteDTO> datos = reporteServicio.reporteVentas(desde, hasta);
        byte[] excel = reporteServicio.exportarVentasExcel(datos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ventas-" + desde + "-" + hasta + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/inventario/excel")
    public ResponseEntity<byte[]> inventarioExcel() throws IOException {
        List<ProductoReporteDTO> datos = reporteServicio.reporteInventario();
        byte[] excel = reporteServicio.exportarInventarioExcel(datos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=inventario-" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/taller/excel")
    public ResponseEntity<byte[]> tallerExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta)
            throws IOException {
        if (desde == null) desde = LocalDate.now().minusMonths(1);
        if (hasta == null) hasta = LocalDate.now();

        List<OTReporteDTO> datos = reporteServicio.reporteTaller(desde, hasta);
        byte[] excel = reporteServicio.exportarTallerExcel(datos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=taller-" + desde + "-" + hasta + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/lista-precios/excel")
    @PreAuthorize("hasAnyRole('DUENO','CAJERA')")
    public ResponseEntity<byte[]> listaPreciosExcel(
            @RequestParam(required = false) Long categoriaId) throws IOException {
        List<ProductoReporteDTO> datos = reporteServicio.listaPrecios(categoriaId);
        byte[] excel = reporteServicio.exportarInventarioExcel(datos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=lista-precios-" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}