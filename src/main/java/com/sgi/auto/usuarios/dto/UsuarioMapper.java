package com.sgi.auto.usuarios.dto;

import com.sgi.auto.usuarios.Usuario;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct genera la implementación automáticamente en tiempo de compilación.
 *
 * builder = @Builder(disableBuilder = true) fuerza a MapStruct a usar
 * el constructor + setters en lugar del patrón Builder de Lombok,
 * porque el UsuarioBuilder no expone los campos heredados de EntidadBase (id, creadoEn, etc.)
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true)
)
public interface UsuarioMapper {

    UsuarioRespuestaDTO aDTO(Usuario usuario);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contrasenaHash", ignore = true)
    @Mapping(target = "estaActivo", constant = "true")
    @Mapping(target = "intentosFallidosLogin", constant = "0")
    Usuario aEntidad(UsuarioCrearDTO dto);
}