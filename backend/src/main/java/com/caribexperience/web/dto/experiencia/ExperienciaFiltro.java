package com.caribexperience.web.dto.experiencia;

import java.time.LocalDate;

/**
 * Criterios opcionales de busqueda para el listado publico de experiencias.
 * Todos los campos son opcionales; los que vengan nulos se ignoran al
 * construir la Specification dinamica (ver ExperienciaSpecifications).
 */
public record ExperienciaFiltro(
        Long ciudadId,
        Long categoriaId,
        LocalDate fechaDesde,
        LocalDate fechaHasta
) {
}
