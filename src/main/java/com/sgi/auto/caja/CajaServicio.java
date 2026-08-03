package com.sgi.auto.caja;

import com.sgi.auto.caja.dto.*;
import com.sgi.auto.compartido.RecursoNoEncontradoExcepcion;
import com.sgi.auto.compartido.ReglaNegocioExcepcion;
import com.sgi.auto.usuarios.Usuario;
import com.sgi.auto.usuarios.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Servicio del módulo de Caja.
 * Apertura de sesión
 * Movimientos automáticos
 * Gastos operativos
 * Cierre de sesión
 * Reporte cierre
 * Historial
 * Egresos autorizados dueño
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CajaServicio {

    private final SesionCajaRepositorio sesionCajaRepositorio;
    private final MovimientoCajaRepositorio movimientoCajaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    // ── Sesión de Caja ────────────────────────────────────────

    @Transactional
    public SesionCajaRespuestaDTO abrirSesion(AperturaCajaDTO solicitud) {
        // Solo DUENO puede abrir caja
        String nombreUsuario = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Usuario usuarioActual = usuarioRepositorio
                .buscarPorNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("Usuario no encontrado"));

        // Determinar para quién se abre
        Usuario cajera = solicitud.cajeraId() != null
                ? usuarioRepositorio.findById(solicitud.cajeraId())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("Cajera no encontrada"))
                : usuarioActual;

        // Verificar que no tenga caja abierta
        sesionCajaRepositorio.buscarSesionAbiertaPorCajera(cajera.getId())
                .ifPresent(s -> {
                    throw new ReglaNegocioExcepcion(
                            cajera.getNombreCompleto() + " ya tiene una sesión de caja abierta");
                });

        SesionCaja sesion = SesionCaja.builder()
                .cajera(cajera)
                .saldoInicialCop(solicitud.saldoInicialCop())
                .build();

        SesionCaja guardada = sesionCajaRepositorio.save(sesion);
        registrarMovimiento(guardada, TipoMovimientoCaja.APERTURA,
                solicitud.saldoInicialCop(), "Saldo inicial de apertura", null);

        log.info("Sesión abierta: cajera={}, saldo={}", cajera.getNombreCompleto(),
                solicitud.saldoInicialCop());
        return aDTO(guardada);
    }

    @Transactional
    public SesionCajaRespuestaDTO cerrarSesion(CierreCajaDTO solicitud) {
        // Buscar la sesión por ID específico
        SesionCaja sesion = sesionCajaRepositorio.findById(solicitud.sesionId())
                .orElseThrow(() -> new ReglaNegocioExcepcion("Sesión no encontrada"));

        if (!sesion.isEstaAbierta()) {
            throw new ReglaNegocioExcepcion("Esta sesión ya está cerrada");
        }

        BigDecimal saldoEsperado = sesion.getSaldoInicialCop()
                .add(sesion.getTotalVentasCop())
                .add(sesion.getTotalAbonosCreditoCop())
                .subtract(sesion.getTotalGastosCop());

        BigDecimal diferencia = solicitud.saldoFinalContadoCop().subtract(saldoEsperado);

        sesion.setSaldoFinalCop(solicitud.saldoFinalContadoCop());
        sesion.setDiferenciaCop(diferencia);
        sesion.setNotasCierre(solicitud.notas());
        sesion.setCerradaEn(OffsetDateTime.now());
        sesion.setEstaAbierta(false);

        SesionCaja cerrada = sesionCajaRepositorio.save(sesion);
        log.info("Sesión cerrada: id={}, cajera={}, diferencia={}",
                cerrada.getId(), cerrada.getCajera().getNombreCompleto(), diferencia);
        return aDTO(cerrada);
    }

    @Transactional(readOnly = true)
    public Page<SesionCajaRespuestaDTO> listarHistorialFiltrado(
            Long cajeraId, LocalDate desde, LocalDate hasta, Pageable pageable) {
        OffsetDateTime desdedt = desde != null
                ? desde.atStartOfDay().atOffset(ZoneOffset.of("-05:00")) : null;
        OffsetDateTime hastaDt = hasta != null
                ? hasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.of("-05:00")) : null;
        return sesionCajaRepositorio
                .listarHistorialFiltrado(cajeraId, desdedt, hastaDt, pageable)
                .map(this::aDTO);
    }

    @Transactional(readOnly = true)
    public SesionCajaRespuestaDTO obtenerSesionActual() {
        String nombreUsuario = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Usuario usuario = usuarioRepositorio.buscarPorNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("Usuario no encontrado"));

        return sesionCajaRepositorio.buscarSesionAbiertaPorCajera(usuario.getId())
                .map(this::aDTO)
                .orElseThrow(() -> new ReglaNegocioExcepcion("No hay sesión de caja abierta"));
    }

    @Transactional(readOnly = true)
    public List<SesionCajaRespuestaDTO> listarSesionesAbiertas() {
        return sesionCajaRepositorio.listarSesionesAbiertas()
                .stream().map(this::aDTO).toList();
    }

    // Historial de sesiones.
    @Transactional(readOnly = true)
    public Page<SesionCajaRespuestaDTO> listarHistorial(Pageable pageable) {
        return sesionCajaRepositorio.listarTodas(pageable).map(this::aDTO);
    }

    @Transactional(readOnly = true)
    public SesionCajaRespuestaDTO obtenerPorId(Long id) {
        return aDTO(buscarSesionOLanzar(id));
    }

    // ── Movimientos de Caja ───────────────────────────────────

    // Registra un gasto operativo.
    @Transactional
    public void registrarGasto(GastoDTO solicitud) {
        String nombreUsuario = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Usuario usuario = usuarioRepositorio.buscarPorNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("Usuario no encontrado"));

        SesionCaja sesion = sesionCajaRepositorio
                .buscarSesionAbiertaPorCajera(usuario.getId())
                .orElseThrow(() -> new ReglaNegocioExcepcion(
                        "No hay ninguna sesión de caja abierta"));

        registrarMovimiento(sesion, TipoMovimientoCaja.GASTO,
                solicitud.montoCop(), solicitud.descripcion(), null);

        sesion.setTotalGastosCop(
                sesion.getTotalGastosCop().add(solicitud.montoCop()));
        sesionCajaRepositorio.save(sesion);

        log.info("Gasto registrado en caja: monto={}, descripcion={}",
                solicitud.montoCop(), solicitud.descripcion());
    }

    @Transactional
    public void registrarEgresoDueno(GastoDTO solicitud) {
        String nombreUsuario = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Usuario usuario = usuarioRepositorio.buscarPorNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("Usuario no encontrado"));

        SesionCaja sesion = sesionCajaRepositorio
                .buscarSesionAbiertaPorCajera(usuario.getId())
                .orElseThrow(() -> new ReglaNegocioExcepcion(
                        "No hay ninguna sesión de caja abierta"));

        registrarMovimiento(sesion, TipoMovimientoCaja.EGRESO_DUENO,
                solicitud.montoCop(),
                "Egreso dueño: " + solicitud.descripcion(), null);

        sesion.setTotalGastosCop(
                sesion.getTotalGastosCop().add(solicitud.montoCop()));
        sesionCajaRepositorio.save(sesion);
    }

    // Registra automáticamente el ingreso de una venta en caja.
    @Transactional
    public void registrarIngresoPorVenta(BigDecimal monto, Long ventaId,
                                         BigDecimal montoEfectivo, BigDecimal montoTransferencia, BigDecimal montoCredito) {

        String nombreUsuario = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Usuario usuario = usuarioRepositorio.buscarPorNombreUsuario(nombreUsuario)
                .orElse(null);

        if (usuario == null) return;

        sesionCajaRepositorio.buscarSesionAbiertaPorCajera(usuario.getId())
                .ifPresent(sesion -> {
                    if (montoEfectivo.compareTo(BigDecimal.ZERO) > 0) {
                        registrarMovimiento(sesion, TipoMovimientoCaja.VENTA,
                                montoEfectivo, "Venta POS #" + ventaId + " - Efectivo", ventaId);
                    }
                    if (montoTransferencia.compareTo(BigDecimal.ZERO) > 0) {
                        registrarMovimiento(sesion, TipoMovimientoCaja.VENTA,
                                montoTransferencia, "Venta POS #" + ventaId + " - Transferencia", ventaId);
                    }
                    sesion.setTotalVentasCop(sesion.getTotalVentasCop().add(monto));
                    sesion.setTotalEfectivoCop(sesion.getTotalEfectivoCop().add(montoEfectivo));
                    sesion.setTotalTransferenciaCop(sesion.getTotalTransferenciaCop().add(montoTransferencia));
                    sesion.setTotalCreditoCop(sesion.getTotalCreditoCop().add(montoCredito));
                    sesionCajaRepositorio.save(sesion);
                });
    }

    // ── Helpers privados ──────────────────────────────────────

    private void registrarMovimiento(
            SesionCaja sesion,
            TipoMovimientoCaja tipo,
            BigDecimal monto,
            String descripcion,
            Long ventaId) {

        String nombreUsuario = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Usuario usuarioActual = usuarioRepositorio
                .buscarPorNombreUsuario(nombreUsuario).orElse(null);

        movimientoCajaRepositorio.save(MovimientoCaja.builder()
                .sesion(sesion)
                .tipo(tipo)
                .montoCop(monto)
                .descripcion(descripcion)
                .ventaId(ventaId)
                .registradoPor(usuarioActual)
                .build());
    }

    private Usuario obtenerUsuarioActual() {
        String nombreUsuario = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return usuarioRepositorio.buscarPorNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "No se encontró el usuario autenticado"));
    }

    private SesionCaja buscarSesionOLanzar(Long id) {
        return sesionCajaRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "No se encontró la sesión de caja con id: " + id));
    }

    private SesionCajaRespuestaDTO aDTO(SesionCaja sesion) {
        List<MovimientoCaja> movimientos =
                movimientoCajaRepositorio.listarPorSesion(sesion.getId());

        BigDecimal saldoEsperado = sesion.getSaldoInicialCop()
                .add(sesion.getTotalVentasCop())
                .add(sesion.getTotalAbonosCreditoCop())
                .subtract(sesion.getTotalGastosCop());

        List<SesionCajaRespuestaDTO.MovimientoRespuestaDTO> movimientosDTO =
                movimientos.stream()
                        .map(m -> new SesionCajaRespuestaDTO.MovimientoRespuestaDTO(
                                m.getId(), m.getTipo(),
                                m.getMontoCop(), m.getDescripcion(),
                                m.getRegistradoPor() != null
                                        ? m.getRegistradoPor().getNombreCompleto()
                                        : null,
                                m.getCreadoEn()))
                        .toList();

        return new SesionCajaRespuestaDTO(
                sesion.getId(),
                sesion.getCajera().getNombreCompleto(),
                sesion.getSaldoInicialCop(),
                sesion.getSaldoFinalCop(),
                sesion.getTotalVentasCop(),
                sesion.getTotalEfectivoCop(),
                sesion.getTotalTransferenciaCop(),
                sesion.getTotalCreditoCop(),
                sesion.getTotalGastosCop(),
                sesion.getTotalAbonosCreditoCop(),
                sesion.getDiferenciaCop(),
                saldoEsperado,
                sesion.isEstaAbierta(),
                sesion.getAbiertaEn(),
                sesion.getCerradaEn(),
                sesion.getNotasCierre(),
                movimientosDTO);
    }
}