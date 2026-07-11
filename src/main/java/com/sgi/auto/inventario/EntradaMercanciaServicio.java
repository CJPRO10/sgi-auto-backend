package com.sgi.auto.inventario;

import com.sgi.auto.compartido.RecursoNoEncontradoExcepcion;
import com.sgi.auto.inventario.dto.EntradaMercanciaCrearDTO;
import com.sgi.auto.inventario.dto.EntradaMercanciaRespuestaDTO;
import com.sgi.auto.usuarios.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntradaMercanciaServicio {

    private final EntradaMercanciaRepositorio entradaRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final ProveedorRepositorio proveedorRepositorio;
    private final MovimientoStockRepositorio movimientoStockRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    @Transactional
    public EntradaMercanciaRespuestaDTO registrar(EntradaMercanciaCrearDTO solicitud) {
        EntradaMercancia entrada = new EntradaMercancia();
        entrada.setNumeroFacturaProveedor(solicitud.numeroFacturaProveedor());
        entrada.setNotas(solicitud.notas());

        if (solicitud.proveedorId() != null) {
            proveedorRepositorio.findById(solicitud.proveedorId())
                    .ifPresent(entrada::setProveedor);
        }

        String nombreUsuario = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        usuarioRepositorio.buscarPorNombreUsuario(nombreUsuario)
                .ifPresent(entrada::setRegistradoPor);

        List<ItemEntrada> items = new ArrayList<>();
        BigDecimal costoTotal = BigDecimal.ZERO;

        for (EntradaMercanciaCrearDTO.ItemEntradaDTO itemDTO : solicitud.items()) {
            Producto producto = productoRepositorio.findById(itemDTO.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                            "Producto no encontrado: " + itemDTO.productoId()));

            BigDecimal costoSinIva = itemDTO.costoUnitarioSinIva() != null
                    ? itemDTO.costoUnitarioSinIva()
                    : itemDTO.costoUnitarioConIva();

            BigDecimal subtotal = itemDTO.costoUnitarioConIva()
                    .multiply(BigDecimal.valueOf(itemDTO.cantidad()));

            ItemEntrada item = ItemEntrada.builder()
                    .entrada(entrada)
                    .producto(producto)
                    .cantidad(itemDTO.cantidad())
                    .costoUnitarioConIva(itemDTO.costoUnitarioConIva())
                    .costoUnitarioSinIva(costoSinIva)
                    .subtotalCop(subtotal)
                    .build();

            items.add(item);
            costoTotal = costoTotal.add(subtotal);

            // Actualizar stock y precio de compra
            int stockAntes = producto.getStockActual();
            producto.setStockActual(stockAntes + itemDTO.cantidad());
            producto.setPrecioCompraConIva(itemDTO.costoUnitarioConIva());
            producto.setPrecioCompraSinIva(costoSinIva);
            productoRepositorio.save(producto);

            // Registrar movimiento en Kardex
            movimientoStockRepositorio.save(MovimientoStock.builder()
                    .producto(producto)
                    .tipoMovimiento(TipoMovimientoStock.ENTRADA)
                    .cantidad(itemDTO.cantidad())
                    .stockAntes(stockAntes)
                    .stockDespues(producto.getStockActual())
                    .costoUnitarioCop(itemDTO.costoUnitarioConIva())
                    .notas("Entrada de mercancía" + (solicitud.numeroFacturaProveedor() != null
                            ? " - Factura: " + solicitud.numeroFacturaProveedor() : ""))
                    .build());
        }

        entrada.setItems(items);
        entrada.setCostoTotalCop(costoTotal);
        EntradaMercancia guardada = entradaRepositorio.save(entrada);

        log.info("Entrada registrada: id={}, items={}, total={}",
                guardada.getId(), items.size(), costoTotal);
        return aDTO(guardada);
    }

    @Transactional(readOnly = true)
    public Page<EntradaMercanciaRespuestaDTO> listar(Pageable pageable) {
        return entradaRepositorio.findAllByOrderByCreadoEnDesc(pageable)
                .map(this::aDTO);
    }

    @Transactional(readOnly = true)
    public EntradaMercanciaRespuestaDTO obtenerPorId(Long id) {
        return entradaRepositorio.findById(id)
                .map(this::aDTO)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "Entrada no encontrada: " + id));
    }

    private EntradaMercanciaRespuestaDTO aDTO(EntradaMercancia e) {
        List<EntradaMercanciaRespuestaDTO.ItemRespuestaDTO> itemsDTO = e.getItems().stream()
                .map(i -> new EntradaMercanciaRespuestaDTO.ItemRespuestaDTO(
                        i.getId(),
                        i.getProducto().getId(),
                        i.getProducto().getNombre(),
                        i.getProducto().getCodigo(),
                        i.getCantidad(),
                        i.getCostoUnitarioConIva(),
                        i.getSubtotalCop()
                )).toList();

        return new EntradaMercanciaRespuestaDTO(
                e.getId(),
                e.getProveedor() != null ? e.getProveedor().getNombre() : null,
                e.getNumeroFacturaProveedor(),
                e.getCostoTotalCop(),
                e.getNotas(),
                e.getRegistradoPor() != null
                        ? e.getRegistradoPor().getNombreCompleto() : null,
                e.getCreadoEn(),
                itemsDTO
        );
    }
}