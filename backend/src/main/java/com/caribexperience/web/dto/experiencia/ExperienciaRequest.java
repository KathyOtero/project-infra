package com.caribexperience.web.dto.experiencia;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos de entrada para crear o actualizar una experiencia. Usado tanto en
 * POST como en PUT (en actualizacion, el guia dueno se valida en el servicio).
 */
public record ExperienciaRequest(

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 150, message = "El titulo no puede superar 150 caracteres")
        String titulo,

        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
        BigDecimal precio,

        @NotNull(message = "El cupo maximo es obligatorio")
        @Positive(message = "El cupo maximo debe ser mayor a 0")
        Integer cupoMax,

        @NotNull(message = "La fecha es obligatoria")
        @FutureOrPresent(message = "La fecha de la experiencia no puede ser en el pasado")
        LocalDate fecha,

        @NotNull(message = "La hora es obligatoria")
        LocalTime hora,

        String fotoUrl,

        @NotNull(message = "La ciudad es obligatoria")
        Long ciudadId,

        @NotNull(message = "La categoria es obligatoria")
        Long categoriaId
) {
}
