package com.sgi.auto.inventario;

import com.sgi.auto.compartido.ApiRespuesta;
import com.sgi.auto.inventario.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DUENO','CAJERA')")
public class ProductoControlador {

    private final ProductoServicio productoServicio;

    // ── Productos ─────────────────────────────────────────────

    @PostMapping("/productos")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<ProductoRespuestaDTO>> crearProducto(
            @Valid @RequestBody ProductoCrearDTO solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRespuesta.exitoso(
                        productoServicio.crearProducto(solicitud),
                        "Producto creado correctamente"));
    }

    @GetMapping("/productos")
    public ResponseEntity<ApiRespuesta<Page<ProductoRespuestaDTO>>> listar(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(productoServicio.listarTodos(pageable)));
    }

    @GetMapping("/productos/{id}")
    public ResponseEntity<ApiRespuesta<ProductoRespuestaDTO>> obtenerPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(productoServicio.obtenerPorId(id)));
    }

    @GetMapping("/productos/codigo/{codigo}")
    public ResponseEntity<ApiRespuesta<ProductoRespuestaDTO>> obtenerPorCodigo(
            @PathVariable String codigo) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(productoServicio.obtenerPorCodigo(codigo)));
    }

    @GetMapping("/productos/buscar")
    public ResponseEntity<ApiRespuesta<List<ProductoRespuestaDTO>>> buscar(
            @RequestParam String q) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(productoServicio.buscar(q)));
    }

    @GetMapping("/productos/stock-bajo")
    public ResponseEntity<ApiRespuesta<List<ProductoRespuestaDTO>>> stockBajo() {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(productoServicio.listarConStockBajoMinimo()));
    }

    // ── Entrada de Mercancía ──────────────────────────────────

    @PostMapping("/entradas")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<Void>> registrarEntrada(
            @Valid @RequestBody EntradaMercanciaDTO solicitud) {
        productoServicio.registrarEntrada(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRespuesta.exitoso(null, "Entrada de mercancía registrada correctamente"));
    }

    // ── Ajuste de Stock ───────────────────────────────────────

    @PatchMapping("/productos/{id}/ajuste-stock")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<ProductoRespuestaDTO>> ajustarStock(
            @PathVariable Long id,
            @Valid @RequestBody AjusteStockDTO solicitud) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                productoServicio.ajustarStock(id, solicitud),
                "Stock ajustado correctamente"));
    }

    // ── Kardex ────────────────────────────────────────────────

    @GetMapping("/productos/{id}/kardex")
    public ResponseEntity<ApiRespuesta<Page<KardexRespuestaDTO>>> kardex(
            @PathVariable Long id,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(productoServicio.obtenerKardex(id, pageable)));
    }

    // ── Categorías ────────────────────────────────────────────

    @GetMapping("/categorias")
    public ResponseEntity<ApiRespuesta<List<Categoria>>> listarCategorias() {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(productoServicio.listarCategorias()));
    }

    @PostMapping("/categorias")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<Categoria>> crearCategoria(
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRespuesta.exitoso(
                        productoServicio.crearCategoria(nombre, descripcion),
                        "Categoría creada correctamente"));
    }

    // ── Proveedores ───────────────────────────────────────────

    @GetMapping("/proveedores")
    public ResponseEntity<ApiRespuesta<List<Proveedor>>> listarProveedores() {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(productoServicio.listarProveedores()));
    }

    @PostMapping("/proveedores")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<Proveedor>> crearProveedor(
            @Valid @RequestBody Proveedor proveedor) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRespuesta.exitoso(
                        productoServicio.crearProveedor(proveedor),
                        "Proveedor creado correctamente"));
    }
}