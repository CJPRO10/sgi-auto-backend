package com.sgi.auto.clientes;

import com.sgi.auto.clientes.dto.*;
import com.sgi.auto.compartido.ConflictoExcepcion;
import com.sgi.auto.compartido.RecursoNoEncontradoExcepcion;
import com.sgi.auto.compartido.ReglaNegocioExcepcion;
import com.sgi.auto.usuarios.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sgi.auto.caja.MovimientoCaja;
import com.sgi.auto.caja.MovimientoCajaRepositorio;
import com.sgi.auto.caja.SesionCajaRepositorio;
import com.sgi.auto.caja.TipoMovimientoCaja;
import com.sgi.auto.usuarios.Usuario;
import com.sgi.auto.usuarios.UsuarioRepositorio;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FidelizacionServicio {

    private final ClienteRepositorio clienteRepositorio;
    private final CreditoRepositorio creditoRepositorio;
    private final HistorialPuntosRepositorio historialPuntosRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final PagoCreditoRepositorio pagoCreditoRepositorio;
    private final SesionCajaRepositorio sesionCajaRepositorio;
    private final MovimientoCajaRepositorio movimientoCajaRepositorio;

    // ── Puntos ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<HistorialPuntosRespuestaDTO> historialPuntos(
            Long clienteId, Pageable pageable) {
        buscarClienteOLanzar(clienteId);
        return historialPuntosRepositorio
                .listarPorCliente(clienteId, pageable)
                .map(this::aHistorialDTO);
    }

    @Transactional
    public ClienteRespuestaDTO ajustarPuntos(Long clienteId, AjustePuntosDTO solicitud) {
        Cliente cliente = buscarClienteOLanzar(clienteId);

        int saldoAntes = cliente.getSaldoPuntos();
        int saldoNuevo = saldoAntes + solicitud.puntos();

        if (saldoNuevo < 0) {
            throw new ReglaNegocioExcepcion(
                    "El ajuste dejaría el saldo de puntos en negativo. " +
                            "Saldo actual: " + saldoAntes + ", ajuste: " + solicitud.puntos());
        }

        cliente.setSaldoPuntos(saldoNuevo);
        clienteRepositorio.save(cliente);

        historialPuntosRepositorio.save(HistorialPuntos.builder()
                .cliente(cliente)
                .tipoMovimiento(TipoMovimientoPuntos.AJUSTE)
                .puntos(Math.abs(solicitud.puntos()))
                .saldoAntes(saldoAntes)
                .saldoDespues(saldoNuevo)
                .descripcion(solicitud.descripcion())
                .build());

        log.info("Ajuste de puntos: clienteId={}, antes={}, despues={}",
                clienteId, saldoAntes, saldoNuevo);

        return aClienteDTO(cliente);
    }

    @Transactional
    public void acumularPuntos(Cliente cliente, int puntos, Long ventaId) {
        if (puntos <= 0) return;

        int saldoAntes = cliente.getSaldoPuntos();
        cliente.setSaldoPuntos(saldoAntes + puntos);
        clienteRepositorio.save(cliente);

        historialPuntosRepositorio.save(HistorialPuntos.builder()
                .cliente(cliente)
                .tipoMovimiento(TipoMovimientoPuntos.ACUMULACION)
                .puntos(puntos)
                .saldoAntes(saldoAntes)
                .saldoDespues(cliente.getSaldoPuntos())
                .descripcion("Puntos por venta #" + ventaId)
                .ventaId(ventaId)
                .build());
    }

    @Transactional
    public void canjearPuntos(Cliente cliente, int puntos, Long ventaId) {
        if (cliente.getSaldoPuntos() < puntos) {
            throw new ReglaNegocioExcepcion(
                    "Puntos insuficientes. Saldo actual: " + cliente.getSaldoPuntos()
                            + ", puntos a canjear: " + puntos);
        }

        int saldoAntes = cliente.getSaldoPuntos();
        cliente.setSaldoPuntos(saldoAntes - puntos);
        clienteRepositorio.save(cliente);

        historialPuntosRepositorio.save(HistorialPuntos.builder()
                .cliente(cliente)
                .tipoMovimiento(TipoMovimientoPuntos.CANJE)
                .puntos(puntos)
                .saldoAntes(saldoAntes)
                .saldoDespues(cliente.getSaldoPuntos())
                .descripcion("Canje en venta #" + ventaId)
                .ventaId(ventaId)
                .build());
    }

    // ── Crédito ───────────────────────────────────────────────

    @Transactional
    public CreditoRespuestaDTO habilitarCredito(Long clienteId,
                                                HabilitarCreditoDTO solicitud) {
        Cliente cliente = buscarClienteOLanzar(clienteId);

        if (creditoRepositorio.existeCreditoActivoPorCliente(clienteId)) {
            throw new ConflictoExcepcion(
                    "El cliente ya tiene un crédito activo");
        }

        var usuarioActual = usuarioRepositorio
                .buscarPorNombreUsuario(
                        SecurityContextHolder.getContext()
                                .getAuthentication().getName())
                .orElse(null);

        Credito credito = Credito.builder()
                .cliente(cliente)
                .montoTotalCop(solicitud.montoTotalCop())
                .aprobadoPor(usuarioActual)
                .build();

        cliente.setCreditoHabilitado(true);
        cliente.setCupoCreditoCop(solicitud.montoTotalCop());
        clienteRepositorio.save(cliente);

        Credito guardado = creditoRepositorio.save(credito);
        log.info("Crédito habilitado: clienteId={}, monto={}",
                clienteId, solicitud.montoTotalCop());

        return aCreditoDTO(guardado);
    }

    @Transactional
    public CreditoRespuestaDTO registrarPago(Long clienteId, PagoCreditoDTO solicitud) {
        Credito credito = creditoRepositorio.buscarActivoPorCliente(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "El cliente no tiene un crédito activo"));

        BigDecimal montoRestante = credito.getMontoTotalCop()
                .subtract(credito.getMontoPagadoCop());

        if (solicitud.montoCop().compareTo(montoRestante) > 0) {
            throw new ReglaNegocioExcepcion(
                    "El pago supera el monto restante de la deuda. " +
                            "Monto restante: " + montoRestante);
        }

        credito.setMontoPagadoCop(
                credito.getMontoPagadoCop().add(solicitud.montoCop()));
        credito.getCliente().setSaldoCreditoCop(
                credito.getMontoTotalCop().subtract(credito.getMontoPagadoCop())
        );
        clienteRepositorio.save(credito.getCliente());
        if (credito.getMontoPagadoCop().compareTo(credito.getMontoTotalCop()) >= 0) {
            credito.setEstaActivo(false);
            credito.getCliente().setCreditoHabilitado(false);
            clienteRepositorio.save(credito.getCliente());
        }

        PagoCredito pago = PagoCredito.builder()
                .credito(credito)
                .montoCop(solicitud.montoCop())
                .notas(solicitud.notas())
                .metodoPago(solicitud.metodoPago())
                .build();

        pagoCreditoRepositorio.save(pago);
        Credito actualizado = creditoRepositorio.save(credito);

        // Registrar en caja
        String nombreUsuario = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Usuario usuario = usuarioRepositorio.buscarPorNombreUsuario(nombreUsuario)
                .orElse(null);

        if (usuario != null) {
            sesionCajaRepositorio.buscarSesionAbiertaPorCajera(usuario.getId())
                    .ifPresent(sesion -> {
                        movimientoCajaRepositorio.save(MovimientoCaja.builder()
                                .sesion(sesion)
                                .tipo(TipoMovimientoCaja.ABONO_CREDITO)
                                .montoCop(solicitud.montoCop())
                                .descripcion("Abono crédito - " +
                                        credito.getCliente().getNombreCompleto() +
                                        " - " + solicitud.metodoPago())
                                .registradoPor(usuario)
                                .build());

                        sesion.setTotalAbonosCreditoCop(
                                sesion.getTotalAbonosCreditoCop().add(solicitud.montoCop()));

                        // Desglose por método
                        if ("EFECTIVO".equals(solicitud.metodoPago())) {
                            sesion.setTotalEfectivoCop(
                                    sesion.getTotalEfectivoCop().add(solicitud.montoCop()));
                        } else if ("TRANSFERENCIA".equals(solicitud.metodoPago())) {
                            sesion.setTotalTransferenciaCop(
                                    sesion.getTotalTransferenciaCop().add(solicitud.montoCop()));
                        }

                        sesionCajaRepositorio.save(sesion);
                    });
        }

        log.info("Pago de crédito: clienteId={}, monto={}, metodo={}",
                clienteId, solicitud.montoCop(), solicitud.metodoPago());
        return aCreditoDTO(actualizado);
    }
    @Transactional(readOnly = true)
    public CreditoRespuestaDTO obtenerCreditoActivo(Long clienteId) {
        return creditoRepositorio.buscarActivoPorCliente(clienteId)
                .map(this::aCreditoDTO)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "El cliente no tiene un crédito activo"));
    }

    @Transactional
    public CreditoRespuestaDTO agregarDeudaManual(Long clienteId, HabilitarCreditoDTO solicitud) {
        Cliente cliente = buscarClienteOLanzar(clienteId);

        Credito credito = creditoRepositorio.buscarActivoPorCliente(clienteId)
                .orElseGet(() -> {
                    Credito nuevo = Credito.builder()
                            .cliente(cliente)
                            .montoTotalCop(BigDecimal.ZERO)
                            .estaActivo(true)
                            .build();
                    cliente.setCreditoHabilitado(true);
                    clienteRepositorio.save(cliente);
                    return creditoRepositorio.save(nuevo);
                });

        credito.setMontoTotalCop(credito.getMontoTotalCop().add(solicitud.montoTotalCop()));
        Credito actualizado = creditoRepositorio.save(credito);
        cliente.setCupoCreditoCop(actualizado.getMontoTotalCop());
        cliente.setSaldoCreditoCop(
                actualizado.getMontoTotalCop().subtract(actualizado.getMontoPagadoCop())
        );
        clienteRepositorio.save(cliente);
        PagoCredito movimiento = PagoCredito.builder()
                .credito(actualizado)
                .montoCop(solicitud.montoTotalCop().negate())
                .notas("DEUDA: " + (solicitud.notas() != null ? solicitud.notas() : "Deuda manual"))
                .build();
        pagoCreditoRepositorio.save(movimiento);

        // Registrar egreso en caja
        String nombreUsuario = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Usuario usuario = usuarioRepositorio.buscarPorNombreUsuario(nombreUsuario)
                .orElse(null);

        if (usuario != null) {
            sesionCajaRepositorio.buscarSesionAbiertaPorCajera(usuario.getId())
                    .ifPresent(sesion -> {
                        movimientoCajaRepositorio.save(MovimientoCaja.builder()
                                .sesion(sesion)
                                .tipo(TipoMovimientoCaja.EGRESO_DUENO)
                                .montoCop(solicitud.montoTotalCop())
                                .descripcion("Préstamo a cliente: " +
                                        cliente.getNombreCompleto() +
                                        (solicitud.notas() != null ? " - " + solicitud.notas() : ""))
                                .registradoPor(usuario)
                                .build());

                        sesion.setTotalGastosCop(
                                sesion.getTotalGastosCop().add(solicitud.montoTotalCop()));
                        sesionCajaRepositorio.save(sesion);
                    });
        }

        log.info("Deuda manual agregada: clienteId={}, monto={}", clienteId, solicitud.montoTotalCop());
        return aCreditoDTO(creditoRepositorio.findById(actualizado.getId()).orElse(actualizado));
    }
    // ── Helpers privados ──────────────────────────────────────

    private Cliente buscarClienteOLanzar(Long id) {
        return clienteRepositorio.findById(id)
                .filter(c -> !c.estaEliminado())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "No se encontró el cliente con id: " + id));
    }

    private CreditoRespuestaDTO aCreditoDTO(Credito credito) {
        BigDecimal restante = credito.getMontoTotalCop()
                .subtract(credito.getMontoPagadoCop());

        List<CreditoRespuestaDTO.MovimientoCreditoDTO> movimientos = new ArrayList<>();

        credito.getPagos().forEach(p ->
                movimientos.add(new CreditoRespuestaDTO.MovimientoCreditoDTO(
                        p.getMontoCop().compareTo(BigDecimal.ZERO) < 0 ? "DEUDA" : "ABONO",
                        p.getMontoCop(),
                        p.getNotas(),
                        p.getCreadoEn()
                ))
        );

        movimientos.sort(Comparator.comparing(
                CreditoRespuestaDTO.MovimientoCreditoDTO::fecha,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        return new CreditoRespuestaDTO(
                credito.getId(),
                credito.getCliente().getId(),
                credito.getCliente().getNombreCompleto(),
                credito.getMontoTotalCop(),
                credito.getMontoPagadoCop(),
                restante,
                credito.isEstaActivo(),
                credito.getCreadoEn(),
                movimientos);
    }

    private HistorialPuntosRespuestaDTO aHistorialDTO(HistorialPuntos h) {
        return new HistorialPuntosRespuestaDTO(
                h.getId(), h.getTipoMovimiento(),
                h.getPuntos(), h.getSaldoAntes(),
                h.getSaldoDespues(), h.getDescripcion(),
                h.getCreadoEn());
    }

    private ClienteRespuestaDTO aClienteDTO(Cliente cliente) {
        return new ClienteRespuestaDTO(
                cliente.getId(), cliente.getNombreCompleto(),
                cliente.getTipoIdentificacion(), cliente.getNumeroIdentificacion(),
                cliente.getDireccion(), cliente.getCelular(), cliente.getCorreo(),
                cliente.isCreditoHabilitado(), cliente.getCupoCreditoCop(),
                cliente.getSaldoCreditoCop(), cliente.getSaldoPuntos(),
                cliente.getCreadoEn());
    }
}