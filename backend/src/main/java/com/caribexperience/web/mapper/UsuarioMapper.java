package com.caribexperience.web.mapper;

import com.caribexperience.domain.Usuario;
import com.caribexperience.web.dto.usuario.UsuarioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "rol", source = "rol.nombre")
    UsuarioResponse toResponse(Usuario usuario);
}
