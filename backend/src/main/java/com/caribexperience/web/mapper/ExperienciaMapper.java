package com.caribexperience.web.mapper;

import com.caribexperience.domain.Experiencia;
import com.caribexperience.web.dto.experiencia.ExperienciaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExperienciaMapper {

    @Mapping(target = "guiaId", source = "guia.id")
    @Mapping(target = "guiaNombre", expression = "java(experiencia.getGuia().getNombre() + \" \" + experiencia.getGuia().getApellido())")
    @Mapping(target = "ciudadId", source = "ciudad.id")
    @Mapping(target = "ciudad", source = "ciudad.nombre")
    @Mapping(target = "categoriaId", source = "categoria.id")
    @Mapping(target = "categoria", source = "categoria.nombre")
    @Mapping(target = "estado", source = "estado.nombre")
    ExperienciaResponse toResponse(Experiencia experiencia);
}
