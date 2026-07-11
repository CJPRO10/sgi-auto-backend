package com.sgi.auto.inventario;

import com.sgi.auto.inventario.dto.EntradaMercanciaCrearDTO;
import com.sgi.auto.inventario.dto.EntradaMercanciaRespuestaDTO;
import com.sgi.auto.usuarios.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntradaMercanciaServicio — pruebas unitarias")
class EntradaMercanciaServicioPrueba {

    @Mock EntradaMercanciaRepositorio entradaRepositorio;
    @Mock ProductoRepositorio productoRepositorio;
    @Mock ProveedorRepositorio proveedorRepositorio;
    @Mock MovimientoStockRepositorio movimientoStockRepositorio;
    @Mock UsuarioRepositorio usuarioRepositorio;

    @InjectMocks EntradaMercanciaServicio entradaServicio;

    private Producto productoPrueba;
    private EntradaMercanciaCrearDTO solicitudPrueba;

    @BeforeEach
    void configurar() {
        productoPrueba = new Producto();
        productoPrueba.setId(1L);
        productoPrueba.setNombre("Alternador 12V");
        productoPrueba.setCodigo("ALT-001");
        productoPrueba.setStockActual(10);
        productoPrueba.setStockMinimo(3);
        productoPrueba.setEstaActivo(true);
        productoPrueba.setPrecioCompraConIva(new BigDecimal("50000"));
        productoPrueba.setPrecioCompraSinIva(new BigDecimal("50000"));
        productoPrueba.setPrecioVentaDetal(new BigDecimal("80000"));
        productoPrueba.setPrecioVentaMayor(new BigDecimal("80000"));

        solicitudPrueba = new EntradaMercanciaCrearDTO(
                null, "FAC-001", "Entrada de prueba",
                List.of(new EntradaMercanciaCrearDTO.ItemEntradaDTO(
                        1L, 20,
                        new BigDecimal("50000"),
                        new BigDecimal("43103")
                ))
        );
    }

    private void mockearUsuarioActual() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    @DisplayName("Registrar entrada actualiza stock y registra movimiento en Kardex")
    void registrar_itemValido_actualizaStockYRegistraMovimiento() {
        mockearUsuarioActual();
        when(productoRepositorio.findById(1L)).thenReturn(Optional.of(productoPrueba));
        when(productoRepositorio.save(any())).thenReturn(productoPrueba);
        when(movimientoStockRepositorio.save(any())).thenReturn(new MovimientoStock());
        when(usuarioRepositorio.buscarPorNombreUsuario("admin")).thenReturn(Optional.empty());

        EntradaMercancia entradaGuardada = new EntradaMercancia();
        entradaGuardada.setId(1L);
        entradaGuardada.setCostoTotalCop(new BigDecimal("1000000"));
        entradaGuardada.setItems(new ArrayList<>());
        entradaGuardada.setCreadoEn(OffsetDateTime.now());
        when(entradaRepositorio.save(any())).thenReturn(entradaGuardada);

        EntradaMercanciaRespuestaDTO resultado = entradaServicio.registrar(solicitudPrueba);

        assertThat(resultado).isNotNull();
        assertThat(productoPrueba.getStockActual()).isEqualTo(30);
        verify(movimientoStockRepositorio).save(any(MovimientoStock.class));
        verify(entradaRepositorio).save(any(EntradaMercancia.class));
    }

    @Test
    @DisplayName("Registrar entrada con proveedor lo asocia correctamente")
    void registrar_conProveedor_asociaProveedor() {
        mockearUsuarioActual();
        Proveedor proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Proveedor Test");

        EntradaMercanciaCrearDTO conProveedor = new EntradaMercanciaCrearDTO(
                1L, "FAC-002", "Con proveedor",
                List.of(new EntradaMercanciaCrearDTO.ItemEntradaDTO(
                        1L, 5, new BigDecimal("50000"), null))
        );

        when(productoRepositorio.findById(1L)).thenReturn(Optional.of(productoPrueba));
        when(productoRepositorio.save(any())).thenReturn(productoPrueba);
        when(movimientoStockRepositorio.save(any())).thenReturn(new MovimientoStock());
        when(proveedorRepositorio.findById(1L)).thenReturn(Optional.of(proveedor));
        when(usuarioRepositorio.buscarPorNombreUsuario("admin")).thenReturn(Optional.empty());

        EntradaMercancia entradaGuardada = new EntradaMercancia();
        entradaGuardada.setId(2L);
        entradaGuardada.setProveedor(proveedor);
        entradaGuardada.setCostoTotalCop(new BigDecimal("250000"));
        entradaGuardada.setItems(new ArrayList<>());
        entradaGuardada.setCreadoEn(OffsetDateTime.now());
        when(entradaRepositorio.save(any())).thenReturn(entradaGuardada);

        EntradaMercanciaRespuestaDTO resultado = entradaServicio.registrar(conProveedor);

        assertThat(resultado.proveedorNombre()).isEqualTo("Proveedor Test");
        verify(proveedorRepositorio).findById(1L);
    }

    @Test
    @DisplayName("Producto no encontrado lanza excepción")
    void registrar_productoInexistente_lanzaExcepcion() {
        mockearUsuarioActual();
        when(productoRepositorio.findById(99L)).thenReturn(Optional.empty());
        when(usuarioRepositorio.buscarPorNombreUsuario("admin")).thenReturn(Optional.empty());

        EntradaMercanciaCrearDTO invalida = new EntradaMercanciaCrearDTO(
                null, null, null,
                List.of(new EntradaMercanciaCrearDTO.ItemEntradaDTO(
                        99L, 5, new BigDecimal("50000"), null))
        );

        assertThatThrownBy(() -> entradaServicio.registrar(invalida))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Listar entradas retorna página correctamente")
    void listar_retornaPaginaDeEntradas() {
        EntradaMercancia entrada = new EntradaMercancia();
        entrada.setId(1L);
        entrada.setCostoTotalCop(new BigDecimal("500000"));
        entrada.setItems(new ArrayList<>());
        entrada.setCreadoEn(OffsetDateTime.now());

        Page<EntradaMercancia> pagina = new PageImpl<>(List.of(entrada));
        when(entradaRepositorio.findAllByOrderByCreadoEnDesc(any()))
                .thenReturn(pagina);

        Page<EntradaMercanciaRespuestaDTO> resultado =
                entradaServicio.listar(PageRequest.of(0, 20));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).costoTotalCop())
                .isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("Obtener por ID retorna entrada correcta")
    void obtenerPorId_existente_retornaEntrada() {
        EntradaMercancia entrada = new EntradaMercancia();
        entrada.setId(1L);
        entrada.setNumeroFacturaProveedor("FAC-001");
        entrada.setCostoTotalCop(new BigDecimal("1000000"));
        entrada.setItems(new ArrayList<>());
        entrada.setCreadoEn(OffsetDateTime.now());

        when(entradaRepositorio.findById(1L)).thenReturn(Optional.of(entrada));

        EntradaMercanciaRespuestaDTO resultado = entradaServicio.obtenerPorId(1L);

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.numeroFacturaProveedor()).isEqualTo("FAC-001");
    }

    @Test
    @DisplayName("Obtener por ID inexistente lanza excepción")
    void obtenerPorId_inexistente_lanzaExcepcion() {
        when(entradaRepositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entradaServicio.obtenerPorId(99L))
                .isInstanceOf(Exception.class);
    }
}