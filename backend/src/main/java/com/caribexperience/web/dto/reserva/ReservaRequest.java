package com.caribexperience.web.dto.reserva;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservaRequest(

        @NotNull(message = "El id de la experiencia es obligatorio")
        Long experienciaId,

        @NotNull(message = "La cantidad de personas es obligatoria")
        @Positive(message = "La cantidad de personas debe ser mayor a 0")
        Integer cantidadPersonas
) {
}
