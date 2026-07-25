package com.sgi.auto.inventario;

import com.sgi.auto.compartido.RecursoNoEncontradoExcepcion;
import com.sgi.auto.compartido.ReglaNegocioExcepcion;
import com.sgi.auto.inventario.dto.CreditoProveedorCrearDTO;
import com.sgi.auto.inventario.dto.CreditoProveedorRespuestaDTO;
import com.sgi.auto.inventario.dto.PagoCreditoProveedorDTO;
import com.sgi.auto.usuarios.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditoProveedorServicio {

    private final CreditoProveedorRepositorio creditoRepositorio;
    private final PagoCreditoProveedorRepositorio pagoRepositorio;
    private final ProveedorRepositorio proveedorRepositorio;
    private final EntradaMercanciaRepositorio entradaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    @Transactional
    public CreditoProveedorRespuestaDTO crear(CreditoProveedorCrearDTO solicitud) {
        Proveedor proveedor = proveedorRepositorio.findById(solicitud.proveedorId())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "Proveedor no encontrado: " + solicitud.proveedorId()));

        CreditoProveedor credito = CreditoProveedor.builder()
                .proveedor(proveedor)
                .montoTotalCop(solicitud.montoTotalCop())
                .notas(solicitud.notas())
                .build();

        if (solicitud.entradaId() != null) {
            entradaRepositorio.findById(solicitud.entradaId())
                    .ifPresent(credito::setEntrada);
        }

        CreditoProveedor guardado = creditoRepositorio.save(credito);
        log.info("Crédito proveedor creado: proveedorId={}, monto={}",
                solicitud.proveedorId(), solicitud.montoTotalCop());
        return aDTO(guardado);
    }

    @Transactional
    public CreditoProveedorRespuestaDTO registrarPago(Long creditoId,
                                                      PagoCreditoProveedorDTO solicitud) {
        CreditoProveedor credito = creditoRepositorio.findById(creditoId)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "Crédito no encontrado: " + creditoId));

        BigDecimal restante = credito.getMontoTotalCop()
                .subtract(credito.getMontoPagadoCop());

        if (solicitud.montoCop().compareTo(restante) > 0) {
            throw new ReglaNegocioExcepcion(
                    "El pago supera el monto restante: " + restante);
        }

        String nombreUsuario = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        PagoCreditoProveedor pago = PagoCreditoProveedor.builder()
                .credito(credito)
                .montoCop(solicitud.montoCop())
                .notas(solicitud.notas())
                .build();

        usuarioRepositorio.buscarPorNombreUsuario(nombreUsuario)
                .ifPresent(pago::setRegistradoPor);

        pagoRepositorio.save(pago);

        credito.setMontoPagadoCop(
                credito.getMontoPagadoCop().add(solicitud.montoCop()));

        if (credito.getMontoPagadoCop().compareTo(credito.getMontoTotalCop()) >= 0) {
            credito.setEstaActivo(false);
        }

        CreditoProveedor actualizado = creditoRepositorio.save(credito);
        log.info("Pago registrado: creditoId={}, monto={}", creditoId, solicitud.montoCop());
        return aDTO(actualizado);
    }

    @Transactional(readOnly = true)
    public List<CreditoProveedorRespuestaDTO> listarActivos() {
        return creditoRepositorio.listarActivos().stream()
                .map(this::aDTO).toList();
    }

    @Transactional(readOnly = true)
    public CreditoProveedorRespuestaDTO obtenerPorId(Long id) {
        return creditoRepositorio.findById(id)
                .map(this::aDTO)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "Crédito no encontrado: " + id));
    }

    private CreditoProveedorRespuestaDTO aDTO(CreditoProveedor c) {
        BigDecimal restante = c.getMontoTotalCop().subtract(c.getMontoPagadoCop());

        List<CreditoProveedorRespuestaDTO.PagoDTO> pagosDTO = c.getPagos().stream()
                .map(p -> new CreditoProveedorRespuestaDTO.PagoDTO(
                        p.getId(),
                        p.getMontoCop(),
                        p.getNotas(),
                        p.getRegistradoPor() != null
                                ? p.getRegistradoPor().getNombreCompleto() : null,
                        p.getCreadoEn()
                )).toList();

        return new CreditoProveedorRespuestaDTO(
                c.getId(),
                c.getProveedor().getId(),
                c.getProveedor().getNombre(),
                c.getEntrada() != null ? c.getEntrada().getId() : null,
                c.getMontoTotalCop(),
                c.getMontoPagadoCop(),
                restante,
                c.isEstaActivo(),
                c.getNotas(),
                c.getCreadoEn(),
                pagosDTO
        );
    }
}