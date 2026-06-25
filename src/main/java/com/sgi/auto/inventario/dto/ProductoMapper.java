package com.sgi.auto.inventario.dto;

import com.sgi.auto.inventario.Producto;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true)
)
public interface ProductoMapper {

    @Mapping(target = "categoriaNombre",
            expression = "java(producto.getCategoria() != null ? producto.getCategoria().getNombre() : null)")
    @Mapping(target = "proveedorNombre",
            expression = "java(producto.getProveedor() != null ? producto.getProveedor().getNombre() : null)")
    @Mapping(target = "stockBajoMinimo",
            expression = "java(producto.getStockActual() <= producto.getStockMinimo())")
    ProductoRespuestaDTO aDTO(Producto producto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "proveedor", ignore = true)
    @Mapping(target = "stockActual", constant = "0")
    @Mapping(target = "margenGananciaPct", ignore = true)
    @Mapping(target = "estaActivo", constant = "true")
    Producto aEntidad(ProductoCrearDTO dto);
}