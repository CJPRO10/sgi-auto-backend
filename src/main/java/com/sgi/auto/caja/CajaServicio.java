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
import java.time.OffsetDateTime;
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

    // Abre una nueva sesión de caja.
    @Transactional
    public SesionCajaRespuestaDTO abrirSesion(AperturaCajaDTO solicitud) {
        // Solo puede haber una sesión abierta a la vez
        sesionCajaRepositorio.buscarSesionAbierta().ifPresent(s -> {
            throw new ReglaNegocioExcepcion(
                    "Ya existe una sesión de caja abierta. Ciérrela antes de abrir una nueva.");
        });

        Usuario cajera = obtenerUsuarioActual();

        SesionCaja sesion = SesionCaja.builder()
                .cajera(cajera)
                .saldoInicialCop(solicitud.saldoInicialCop())
                .build();

        SesionCaja guardada = sesionCajaRepositorio.save(sesion);

        // Registrar movimiento de apertura
        registrarMovimiento(guardada, TipoMovimientoCaja.APERTURA,
                solicitud.saldoInicialCop(), "Saldo inicial de apertura", null);

        log.info("Sesión de caja abierta: id={}, cajera={}, saldoInicial={}",
                guardada.getId(), cajera.getNombreCompleto(),
                solicitud.saldoInicialCop());

        return aDTO(guardada);
    }

    // Cierra la sesión activa y calcula la diferencia.
    @Transactional
    public SesionCajaRespuestaDTO cerrarSesion(CierreCajaDTO solicitud) {
        SesionCaja sesion = sesionCajaRepositorio.buscarSesionAbierta()
                .orElseThrow(() -> new ReglaNegocioExcepcion(
                        "No hay ninguna sesión de caja abierta"));

        // Saldo esperado = inicial + ventas + abonos - gastos - egresos
        BigDecimal saldoEsperado = sesion.getSaldoInicialCop()
                .add(sesion.getTotalVentasCop())
                .add(sesion.getTotalAbonosCreditoCop())
                .subtract(sesion.getTotalGastosCop());

        BigDecimal diferencia = solicitud.saldoFinalContadoCop()
                .subtract(saldoEsperado);

        sesion.setSaldoFinalCop(solicitud.saldoFinalContadoCop());
        sesion.setDiferenciaCop(diferencia);
        sesion.setNotasCierre(solicitud.notas());
        sesion.setCerradaEn(OffsetDateTime.now());
        sesion.setEstaAbierta(false);

        SesionCaja cerrada = sesionCajaRepositorio.save(sesion);

        log.info("Sesión de caja cerrada: id={}, diferencia={}",
                cerrada.getId(), diferencia);

        return aDTO(cerrada);
    }

    // Obtiene la sesión actualmente abierta.
    @Transactional(readOnly = true)
    public SesionCajaRespuestaDTO obtenerSesionActual() {
        return sesionCajaRepositorio.buscarSesionAbierta()
                .map(this::aDTO)
                .orElseThrow(() -> new ReglaNegocioExcepcion(
                        "No hay ninguna sesión de caja abierta"));
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
        SesionCaja sesion = sesionCajaRepositorio.buscarSesionAbierta()
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

    // Registra un egreso autorizado por el dueño.
    @Transactional
    public void registrarEgresoDueno(GastoDTO solicitud) {
        SesionCaja sesion = sesionCajaRepositorio.buscarSesionAbierta()
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
    public void registrarIngresoPorVenta(BigDecimal monto, Long ventaId) {
        sesionCajaRepositorio.buscarSesionAbierta().ifPresent(sesion -> {
            registrarMovimiento(sesion, TipoMovimientoCaja.VENTA,
                    monto, "Venta POS #" + ventaId, ventaId);
            sesion.setTotalVentasCop(sesion.getTotalVentasCop().add(monto));
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

        movimientoCajaRepositorio.save(MovimientoCaja.builder()
                .sesion(sesion)
                .tipo(tipo)
                .montoCop(monto)
                .descripcion(descripcion)
                .ventaId(ventaId)
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
                                m.getCreadoEn()))
                        .toList();

        return new SesionCajaRespuestaDTO(
                sesion.getId(),
                sesion.getCajera().getNombreCompleto(),
                sesion.getSaldoInicialCop(),
                sesion.getSaldoFinalCop(),
                sesion.getTotalVentasCop(),
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