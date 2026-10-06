package com.caribexperience.web.dto.reserva;

import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,
        Long experienciaId,
        String experienciaTitulo,
        Long viajeroId,
        String viajeroNombre,
        Integer cantidadPersonas,
        String estado,
        LocalDateTime fechaReserva
) {
}
