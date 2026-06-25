package com.sgi.auto.clientes.dto;

import com.sgi.auto.clientes.Cliente;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true)
)
public interface ClienteMapper {
    ClienteRespuestaDTO aDTO(Cliente cliente);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creditoHabilitado", constant = "false")
    @Mapping(target = "cupoCreditoCop", constant = "0")
    @Mapping(target = "saldoCreditoCop", constant = "0")
    @Mapping(target = "saldoPuntos", constant = "0")
    Cliente aEntidad(ClienteCrearDTO dto);
}