package com.sgi.auto.pos;

import com.sgi.auto.caja.CajaServicio;
import com.sgi.auto.clientes.Cliente;
import com.sgi.auto.clientes.ClienteRepositorio;
import com.sgi.auto.clientes.CreditoRepositorio;
import com.sgi.auto.compartido.ConflictoExcepcion;
import com.sgi.auto.compartido.RecursoNoEncontradoExcepcion;
import com.sgi.auto.compartido.ReglaNegocioExcepcion;
import com.sgi.auto.inventario.MovimientoStock;
import com.sgi.auto.inventario.MovimientoStockRepositorio;
import com.sgi.auto.inventario.Producto;
import com.sgi.auto.inventario.ProductoRepositorio;
import com.sgi.auto.inventario.TipoMovimientoStock;
import com.sgi.auto.pos.dto.AnulacionDTO;
import com.sgi.auto.pos.dto.VentaCrearDTO;
import com.sgi.auto.pos.dto.VentaRespuestaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio del Punto de Venta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaServicio {

    private final VentaRepositorio ventaRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final MovimientoStockRepositorio movimientoStockRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final CajaServicio cajaServicio;
    private final CreditoRepositorio creditoRepositorio;

    // Regla de negocio: cada COP gastado = 1 punto
    private static final BigDecimal FACTOR_PUNTOS = BigDecimal.ONE;

    @Transactional
    public VentaRespuestaDTO crear(VentaCrearDTO solicitud) {

        // Idempotencia: si ya existe con esta clave, retorna la existente
        var ventaExistente = ventaRepositorio
                .findByClaveIdempotencia(solicitud.claveIdempotencia());
        if (ventaExistente.isPresent()) {
            log.info("Venta duplicada detectada por idempotencia: clave={}",
                    solicitud.claveIdempotencia());
            return aDTO(ventaExistente.get());
        }

        Venta venta = new Venta();
        venta.setClaveIdempotencia(solicitud.claveIdempotencia());
        venta.setMetodoPago(solicitud.metodoPago());
        venta.setEstado(EstadoVenta.COMPLETADA);
        venta.setDescuentoCop(
                solicitud.descuentoCop() != null
                        ? solicitud.descuentoCop()
                        : BigDecimal.ZERO);
        venta.setPuntosCanjeadosCop(
                solicitud.puntosCanjeadosCop() != null
                        ? solicitud.puntosCanjeadosCop()
                        : BigDecimal.ZERO);

        // Cliente anónimo o registrado
        if (solicitud.clienteId() != null) {
            Cliente cliente = clienteRepositorio.findById(solicitud.clienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                            "No se encontró el cliente con id: " + solicitud.clienteId()));
            venta.setCliente(cliente);
        } else {
            venta.setNombreClienteAnonimo(
                    solicitud.nombreClienteAnonimo() != null
                            ? solicitud.nombreClienteAnonimo()
                            : "Cliente general");
        }

        // Procesar items
        List<ItemVenta> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (VentaCrearDTO.ItemVentaDTO itemDTO : solicitud.items()) {
            Producto producto = productoRepositorio.findById(itemDTO.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                            "No se encontró el producto con id: " + itemDTO.productoId()));

            // Verificar stock suficiente
            if (producto.getStockActual() < itemDTO.cantidad()) {
                throw new ReglaNegocioExcepcion(
                        "Stock insuficiente para el producto '" + producto.getNombre()
                                + "'. Stock actual: " + producto.getStockActual()
                                + ", solicitado: " + itemDTO.cantidad());
            }

            BigDecimal descuentoUnitario = itemDTO.descuentoUnitarioCop() != null
                    ? itemDTO.descuentoUnitarioCop()
                    : BigDecimal.ZERO;

            BigDecimal subtotalItem = itemDTO.precioUnitarioCop()
                    .subtract(descuentoUnitario)
                    .multiply(BigDecimal.valueOf(itemDTO.cantidad()));

            ItemVenta item = ItemVenta.builder()
                    .venta(venta)
                    .producto(producto)
                    .nombreProductoSnapshot(producto.getNombre())
                    .codigoProductoSnapshot(producto.getCodigo())
                    .cantidad(itemDTO.cantidad())
                    .precioUnitarioCop(itemDTO.precioUnitarioCop())
                    .descuentoUnitarioCop(descuentoUnitario)
                    .subtotalCop(subtotalItem)
                    .build();

            items.add(item);
            subtotal = subtotal.add(subtotalItem);

            // Descontar stock @Transactional
            int stockAntes = producto.getStockActual();
            producto.setStockActual(stockAntes - itemDTO.cantidad());
            productoRepositorio.save(producto);

            movimientoStockRepositorio.save(MovimientoStock.builder()
                    .producto(producto)
                    .tipoMovimiento(TipoMovimientoStock.VENTA)
                    .cantidad(itemDTO.cantidad())
                    .stockAntes(stockAntes)
                    .stockDespues(producto.getStockActual())
                    .costoUnitarioCop(itemDTO.precioUnitarioCop())
                    .notas("Venta POS - " + solicitud.claveIdempotencia())
                    .build());
        }

        venta.setItems(items);
        venta.setSubtotalCop(subtotal);

        // Calcular total
        BigDecimal total = subtotal
                .subtract(venta.getDescuentoCop())
                .subtract(venta.getPuntosCanjeadosCop());
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;
        venta.setTotalCop(total);

        // Calcular vuelto
        BigDecimal montoPagado = solicitud.montoPagadoCop() != null
                ? solicitud.montoPagadoCop()
                : total;
        venta.setMontoPagadoCop(montoPagado);
        venta.setVueltoCop(montoPagado.subtract(total).max(BigDecimal.ZERO));

        // Acumular puntos (solo con cliente registrado)
        if (venta.getCliente() != null) {
            int puntosGanados = total.multiply(FACTOR_PUNTOS).intValue();
            venta.setPuntosGanados(puntosGanados);
            Cliente cliente = venta.getCliente();
            cliente.setSaldoPuntos(cliente.getSaldoPuntos() + puntosGanados);
            clienteRepositorio.save(cliente);
        }

        Venta guardada = ventaRepositorio.save(venta);
        log.info("Venta creada: id={}, total={}, items={}",
                guardada.getId(), guardada.getTotalCop(), items.size());

        // Registrar en caja si hay sesión abierta
        cajaServicio.registrarIngresoPorVenta(total, guardada.getId());

// Si el pago es a crédito, registrar deuda
        if (solicitud.metodoPago() == MetodoPago.CREDITO && venta.getCliente() != null) {
            creditoRepositorio.buscarActivoPorCliente(venta.getCliente().getId())
                    .ifPresentOrElse(
                            credito -> {
                                credito.setMontoTotalCop(credito.getMontoTotalCop().add(total));
                                creditoRepositorio.save(credito);
                            },
                            () -> {
                                // Si no tiene crédito activo, crear uno nuevo
                                Credito credito = Credito.builder()
                                        .cliente(venta.getCliente())
                                        .montoTotalCop(total)
                                        .estaActivo(true)
                                        .build();
                                venta.getCliente().setCreditoHabilitado(true);
                                venta.getCliente().setCupoCreditoCop(total);
                                clienteRepositorio.save(venta.getCliente());
                                creditoRepositorio.save(credito);
                            }
                    );
        }
        return aDTO(guardada);
    }

    //Anula una venta y revierte el stock.
    @Transactional
    public VentaRespuestaDTO anular(Long ventaId, AnulacionDTO solicitud) {
        Venta venta = ventaRepositorio.findById(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "No se encontró la venta con id: " + ventaId));

        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new ReglaNegocioExcepcion(
                    "La venta ya está anulada");
        }

        // Revertir stock de cada item
        for (ItemVenta item : venta.getItems()) {
            Producto producto = item.getProducto();
            int stockAntes = producto.getStockActual();
            producto.setStockActual(stockAntes + item.getCantidad());
            productoRepositorio.save(producto);

            movimientoStockRepositorio.save(MovimientoStock.builder()
                    .producto(producto)
                    .tipoMovimiento(TipoMovimientoStock.DEVOLUCION_SALIDA)
                    .cantidad(item.getCantidad())
                    .stockAntes(stockAntes)
                    .stockDespues(producto.getStockActual())
                    .notas("Anulación de venta #" + ventaId + ": " + solicitud.razon())
                    .build());
        }

        // Revertir puntos si aplica
        if (venta.getCliente() != null && venta.getPuntosGanados() > 0) {
            Cliente cliente = venta.getCliente();
            cliente.setSaldoPuntos(
                    Math.max(0, cliente.getSaldoPuntos() - venta.getPuntosGanados()));
            clienteRepositorio.save(cliente);
        }

        venta.setEstado(EstadoVenta.ANULADA);
        venta.setRazonAnulacion(solicitud.razon());
        venta.setAnuladaEn(OffsetDateTime.now());

        Venta anulada = ventaRepositorio.save(venta);
        log.info("Venta anulada: id={}, razón={}", ventaId, solicitud.razon());
        return aDTO(anulada);
    }

    //Obtiene una venta por su id.
    @Transactional(readOnly = true)
    public VentaRespuestaDTO obtenerPorId(Long id) {
        return aDTO(ventaRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "No se encontró la venta con id: " + id)));
    }

    //Historial de ventas de un cliente.
    @Transactional(readOnly = true)
    public Page<VentaRespuestaDTO> ventasPorCliente(Long clienteId, Pageable pageable) {
        return ventaRepositorio.ventasPorCliente(clienteId, pageable)
                .map(this::aDTO);
    }

    //Ventas del día.
    @Transactional(readOnly = true)
    public Page<VentaRespuestaDTO> ventasDeHoy(Pageable pageable) {
        OffsetDateTime inicioDia = LocalDate.now()
                .atStartOfDay()
                .atOffset(ZoneOffset.of("-05:00"));
        return ventaRepositorio.ventasDeHoy(inicioDia, pageable).map(this::aDTO);
    }

    // ── Mapper manual ──

    private VentaRespuestaDTO aDTO(Venta venta) {
        String nombreCliente = venta.getCliente() != null
                ? venta.getCliente().getNombreCompleto()
                : venta.getNombreClienteAnonimo();

        List<VentaRespuestaDTO.ItemVentaRespuestaDTO> itemsDTO = venta.getItems()
                .stream()
                .map(item -> new VentaRespuestaDTO.ItemVentaRespuestaDTO(
                        item.getProducto() != null ? item.getProducto().getId() : null,
                        item.getNombreProductoSnapshot(),
                        item.getCodigoProductoSnapshot(),
                        item.getCantidad(),
                        item.getPrecioUnitarioCop(),
                        item.getSubtotalCop()))
                .toList();

        return new VentaRespuestaDTO(
                venta.getId(),
                venta.getClaveIdempotencia(),
                nombreCliente,
                venta.getMetodoPago(),
                venta.getEstado(),
                venta.getSubtotalCop(),
                venta.getDescuentoCop(),
                venta.getTotalCop(),
                venta.getMontoPagadoCop(),
                venta.getVueltoCop(),
                venta.getPuntosGanados(),
                itemsDTO,
                venta.getCreadoEn());
    }
}