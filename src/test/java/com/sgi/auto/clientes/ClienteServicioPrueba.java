package com.sgi.auto.clientes;

import com.sgi.auto.clientes.dto.ClienteCrearDTO;
import com.sgi.auto.clientes.dto.ClienteMapper;
import com.sgi.auto.clientes.dto.ClienteRespuestaDTO;
import com.sgi.auto.compartido.ConflictoExcepcion;
import com.sgi.auto.compartido.RecursoNoEncontradoExcepcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteServicio — pruebas unitarias")
class ClienteServicioPrueba {

    @Mock ClienteRepositorio clienteRepositorio;
    @Mock ClienteMapper clienteMapper;

    @InjectMocks ClienteServicio clienteServicio;

    private Cliente clientePrueba;
    private ClienteCrearDTO solicitudCrear;
    private ClienteRespuestaDTO respuestaPrueba;

    @BeforeEach
    void configurar() {
        clientePrueba = new Cliente();
        clientePrueba.setId(1L);
        clientePrueba.setNombreCompleto("Juan García");
        clientePrueba.setNumeroIdentificacion("12345678");
        clientePrueba.setTipoIdentificacion("CC");
        clientePrueba.setSaldoPuntos(0);
        clientePrueba.setCupoCreditoCop(BigDecimal.ZERO);
        clientePrueba.setSaldoCreditoCop(BigDecimal.ZERO);

        solicitudCrear = new ClienteCrearDTO(
                "Juan García", "CC", "12345678",
                "Calle 10 #5-20", "300 123 4567", "juan@email.com");

        respuestaPrueba = new ClienteRespuestaDTO(
                1L, "Juan García", "CC", "12345678",
                "Calle 10 #5-20", "300 123 4567", "juan@email.com",
                false, BigDecimal.ZERO, BigDecimal.ZERO, 0, null);
    }

    @Test
    @DisplayName("Crear cliente exitosamente")
    void crear_clienteValido_seGuarda() {
        when(clienteRepositorio.existePorNumeroIdentificacion("12345678"))
                .thenReturn(false);
        when(clienteMapper.aEntidad(solicitudCrear)).thenReturn(clientePrueba);
        when(clienteRepositorio.save(clientePrueba)).thenReturn(clientePrueba);
        when(clienteMapper.aDTO(clientePrueba)).thenReturn(respuestaPrueba);

        ClienteRespuestaDTO resultado = clienteServicio.crear(solicitudCrear);

        assertThat(resultado.numeroIdentificacion()).isEqualTo("12345678");
        assertThat(resultado.nombreCompleto()).isEqualTo("Juan García");
        verify(clienteRepositorio).save(clientePrueba);
    }

    @Test
    @DisplayName("Crear cliente con identificación duplicada lanza ConflictoExcepcion")
    void crear_identificacionDuplicada_lanzaConflicto() {
        when(clienteRepositorio.existePorNumeroIdentificacion("12345678"))
                .thenReturn(true);

        assertThatThrownBy(() -> clienteServicio.crear(solicitudCrear))
                .isInstanceOf(ConflictoExcepcion.class)
                .hasMessageContaining("12345678");

        verify(clienteRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Buscar con término muy corto retorna lista vacía")
    void buscar_terminoMuyCorto_retornaVacio() {
        List<ClienteRespuestaDTO> resultado = clienteServicio.buscar("a");
        assertThat(resultado).isEmpty();
        verifyNoInteractions(clienteRepositorio);
    }

    @Test
    @DisplayName("Obtener cliente inexistente lanza RecursoNoEncontradoExcepcion")
    void obtenerPorId_noExiste_lanzaExcepcion() {
        when(clienteRepositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteServicio.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoExcepcion.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Eliminar cliente aplica soft delete")
    void eliminar_clienteExistente_marcaEliminado() {
        when(clienteRepositorio.findById(1L))
                .thenReturn(Optional.of(clientePrueba));
        when(clienteRepositorio.save(clientePrueba)).thenReturn(clientePrueba);

        clienteServicio.eliminar(1L);

        assertThat(clientePrueba.getEliminadoEn()).isNotNull();
        verify(clienteRepositorio).save(clientePrueba);
    }
}