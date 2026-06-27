package com.sgi.auto.clientes;

import com.sgi.auto.compartido.ConflictoExcepcion;
import com.sgi.auto.compartido.RecursoNoEncontradoExcepcion;
import com.sgi.auto.clientes.dto.ClienteActualizarDTO;
import com.sgi.auto.clientes.dto.ClienteCrearDTO;
import com.sgi.auto.clientes.dto.ClienteMapper;
import com.sgi.auto.clientes.dto.ClienteRespuestaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteServicio {

    private final ClienteRepositorio clienteRepositorio;
    private final ClienteMapper clienteMapper;

    @Transactional
    public ClienteRespuestaDTO crear(ClienteCrearDTO solicitud) {
        if (clienteRepositorio.existePorNumeroIdentificacion(
                solicitud.numeroIdentificacion())) {
            throw new ConflictoExcepcion(
                    "Ya existe un cliente con identificación: "
                            + solicitud.numeroIdentificacion());
        }

        Cliente cliente = clienteMapper.aEntidad(solicitud);
        Cliente guardado = clienteRepositorio.save(cliente);
        log.info("Cliente creado: id={}, nombre={}",
                guardado.getId(), guardado.getNombreCompleto());
        return clienteMapper.aDTO(guardado);
    }

    @Transactional(readOnly = true)
    public Page<ClienteRespuestaDTO> listarTodos(Pageable pageable) {
        return clienteRepositorio.listarActivos(pageable)
                .map(clienteMapper::aDTO);
    }

    @Transactional(readOnly = true)
    public ClienteRespuestaDTO obtenerPorId(Long id) {
        return clienteMapper.aDTO(buscarOLanzar(id));
    }

    @Transactional(readOnly = true)
    public ClienteRespuestaDTO obtenerPorIdentificacion(String numero) {
        return clienteRepositorio.buscarPorNumeroIdentificacion(numero)
                .map(clienteMapper::aDTO)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "No se encontró cliente con identificación: " + numero));
    }

    // Búsqueda full-text con índice GIN
    @Transactional(readOnly = true)
    public List<ClienteRespuestaDTO> buscar(String termino) {
        if (termino == null || termino.trim().length() < 2) {
            return List.of();
        }
        return clienteRepositorio.buscarPorNombre(termino.trim())
                .stream()
                .map(clienteMapper::aDTO)
                .toList();
    }

    @Transactional
    public ClienteRespuestaDTO actualizar(Long id, ClienteActualizarDTO solicitud) {
        Cliente cliente = buscarOLanzar(id);

        if (solicitud.nombreCompleto() != null)
            cliente.setNombreCompleto(solicitud.nombreCompleto());
        if (solicitud.direccion() != null)
            cliente.setDireccion(solicitud.direccion());
        if (solicitud.celular() != null)
            cliente.setCelular(solicitud.celular());
        if (solicitud.correo() != null)
            cliente.setCorreo(solicitud.correo());
        if (solicitud.tipoIdentificacion() != null)
            cliente.setTipoIdentificacion(solicitud.tipoIdentificacion());

        return clienteMapper.aDTO(clienteRepositorio.save(cliente));
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscarOLanzar(id);
        cliente.setEliminadoEn(java.time.OffsetDateTime.now());
        clienteRepositorio.save(cliente);
        log.info("Cliente eliminado: id={}", id);
    }

    private Cliente buscarOLanzar(Long id) {
        return clienteRepositorio.findById(id)
                .filter(c -> !c.estaEliminado())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "No se encontró el cliente con id: " + id));
    }
}