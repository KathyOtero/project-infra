package com.caribexperience.web.dto.experiencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Representacion de una experiencia expuesta por la API, con los catalogos
 * ya resueltos a texto legible (no se exponen IDs de FK innecesarios,
 * salvo los que el frontend necesita para operar, ej. guiaId).
 */
public record ExperienciaResponse(
        Long id,
        Long guiaId,
        String guiaNombre,
        Long ciudadId,
        String ciudad,
        Long categoriaId,
        String categoria,
        String estado,
        String titulo,
        String descripcion,
        BigDecimal precio,
        Integer cupoMax,
        Integer cupoDisponible,
        LocalDate fecha,
        LocalTime hora,
        String fotoUrl,
        LocalDateTime createdAt
) {
}
