package com.caribexperience.web.mapper;

import com.caribexperience.domain.Reserva;
import com.caribexperience.web.dto.reserva.ReservaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservaMapper {

    @Mapping(target = "experienciaId", source = "experiencia.id")
    @Mapping(target = "experienciaTitulo", source = "experiencia.titulo")
    @Mapping(target = "viajeroId", source = "viajero.id")
    @Mapping(target = "viajeroNombre", expression = "java(reserva.getViajero().getNombre() + \" \" + reserva.getViajero().getApellido())")
    @Mapping(target = "estado", source = "estado.nombre")
    ReservaResponse toResponse(Reserva reserva);
}
