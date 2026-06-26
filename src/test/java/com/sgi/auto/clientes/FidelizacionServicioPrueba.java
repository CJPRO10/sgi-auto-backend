package com.sgi.auto.clientes;

import com.sgi.auto.clientes.dto.*;
import com.sgi.auto.compartido.ConflictoExcepcion;
import com.sgi.auto.compartido.ReglaNegocioExcepcion;
import com.sgi.auto.usuarios.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FidelizacionServicio — pruebas unitarias")
class FidelizacionServicioPrueba {

    @Mock ClienteRepositorio clienteRepositorio;
    @Mock CreditoRepositorio creditoRepositorio;
    @Mock HistorialPuntosRepositorio historialPuntosRepositorio;
    @Mock UsuarioRepositorio usuarioRepositorio;

    @InjectMocks FidelizacionServicio fidelizacionServicio;

    private Cliente clientePrueba;
    private Credito creditoPrueba;

    @BeforeEach
    void configurar() {
        clientePrueba = new Cliente();
        clientePrueba.setId(1L);
        clientePrueba.setNombreCompleto("Juan García");
        clientePrueba.setNumeroIdentificacion("12345678");
        clientePrueba.setTipoIdentificacion("CC");
        clientePrueba.setSaldoPuntos(100);
        clientePrueba.setCreditoHabilitado(false);
        clientePrueba.setCupoCreditoCop(BigDecimal.ZERO);
        clientePrueba.setSaldoCreditoCop(BigDecimal.ZERO);

        creditoPrueba = Credito.builder()
                .cliente(clientePrueba)
                .montoTotalCop(new BigDecimal("500000"))
                .montoPagadoCop(new BigDecimal("200000"))
                .estaActivo(true)
                .pagos(new ArrayList<>())
                .build();
        creditoPrueba.setId(1L);

        var auth = new UsernamePasswordAuthenticationToken(
                "admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_DUENO")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Ajuste positivo de puntos aumenta el saldo y registra historial")
    void ajustarPuntos_positivo_aumentaSaldo() {
        when(clienteRepositorio.findById(1L)).thenReturn(Optional.of(clientePrueba));
        when(clienteRepositorio.save(any())).thenReturn(clientePrueba);
        when(historialPuntosRepositorio.save(any())).thenReturn(new HistorialPuntos());

        fidelizacionServicio.ajustarPuntos(1L,
                new AjustePuntosDTO(50, "Bono especial"));

        assertThat(clientePrueba.getSaldoPuntos()).isEqualTo(150);
        verify(historialPuntosRepositorio).save(any(HistorialPuntos.class));
    }

    @Test
    @DisplayName("Ajuste negativo que deja puntos en negativo lanza ReglaNegocioExcepcion")
    void ajustarPuntos_negativoExcesivo_lanzaReglaNegocio() {
        when(clienteRepositorio.findById(1L)).thenReturn(Optional.of(clientePrueba));

        assertThatThrownBy(() ->
                fidelizacionServicio.ajustarPuntos(1L,
                        new AjustePuntosDTO(-200, "Descuento excesivo")))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .hasMessageContaining("negativo");

        verify(clienteRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Habilitar crédito exitosamente")
    void habilitarCredito_clienteSinCredito_creaCredito() {
        when(clienteRepositorio.findById(1L)).thenReturn(Optional.of(clientePrueba));
        when(creditoRepositorio.existeCreditoActivoPorCliente(1L)).thenReturn(false);
        when(usuarioRepositorio.buscarPorNombreUsuario(any())).thenReturn(Optional.empty());
        when(creditoRepositorio.save(any())).thenReturn(creditoPrueba);

        CreditoRespuestaDTO resultado = fidelizacionServicio.habilitarCredito(1L,
                new HabilitarCreditoDTO(new BigDecimal("500000")));

        assertThat(resultado).isNotNull();
        assertThat(clientePrueba.isCreditoHabilitado()).isTrue();
        verify(creditoRepositorio).save(any(Credito.class));
    }

    @Test
    @DisplayName("Habilitar crédito a cliente que ya tiene uno lanza ConflictoExcepcion")
    void habilitarCredito_clienteConCreditoActivo_lanzaConflicto() {
        when(clienteRepositorio.findById(1L)).thenReturn(Optional.of(clientePrueba));
        when(creditoRepositorio.existeCreditoActivoPorCliente(1L)).thenReturn(true);

        assertThatThrownBy(() ->
                fidelizacionServicio.habilitarCredito(1L,
                        new HabilitarCreditoDTO(new BigDecimal("300000"))))
                .isInstanceOf(ConflictoExcepcion.class)
                .hasMessageContaining("ya tiene un crédito activo");
    }

    @Test
    @DisplayName("Pago de crédito actualiza monto pagado")
    void registrarPago_montoValido_actualizaMontoPagado() {
        when(creditoRepositorio.buscarActivoPorCliente(1L))
                .thenReturn(Optional.of(creditoPrueba));
        when(creditoRepositorio.save(any())).thenReturn(creditoPrueba);

        fidelizacionServicio.registrarPago(1L,
                new PagoCreditoDTO(new BigDecimal("100000"), "Abono mensual"));

        assertThat(creditoPrueba.getMontoPagadoCop())
                .isEqualByComparingTo("300000");
    }

    @Test
    @DisplayName("Pago que supera la deuda lanza ReglaNegocioExcepcion")
    void registrarPago_montoExcesivo_lanzaReglaNegocio() {
        when(creditoRepositorio.buscarActivoPorCliente(1L))
                .thenReturn(Optional.of(creditoPrueba));

        assertThatThrownBy(() ->
                fidelizacionServicio.registrarPago(1L,
                        new PagoCreditoDTO(new BigDecimal("500000"), "Pago excesivo")))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .hasMessageContaining("supera el monto restante");
    }

    @Test
    @DisplayName("Canje de puntos insuficientes lanza ReglaNegocioExcepcion")
    void canjearPuntos_puntosInsuficientes_lanzaReglaNegocio() {
        assertThatThrownBy(() ->
                fidelizacionServicio.canjearPuntos(clientePrueba, 200, 1L))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .hasMessageContaining("Puntos insuficientes");
    }
}