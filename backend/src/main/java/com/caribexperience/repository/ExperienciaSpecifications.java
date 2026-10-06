package com.caribexperience.repository;

import com.caribexperience.common.EstadoExperienciaNombre;
import com.caribexperience.domain.Experiencia;
import com.caribexperience.web.dto.experiencia.ExperienciaFiltro;
import org.springframework.data.jpa.domain.Specification;

/**
 * Construye la Specification dinamica para el listado publico de
 * experiencias segun los filtros opcionales recibidos. Mantiene el
 * repositorio (y el servicio) libres de armar Predicates a mano.
 */
public final class ExperienciaSpecifications {

    private ExperienciaSpecifications() {
    }

    public static Specification<Experiencia> conFiltro(ExperienciaFiltro filtro) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            // Solo se muestran experiencias activas en el listado publico
            predicates = cb.and(predicates, cb.equal(root.get("estado").get("nombre"), EstadoExperienciaNombre.ACTIVA));

            if (filtro.ciudadId() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("ciudad").get("id"), filtro.ciudadId()));
            }
            if (filtro.categoriaId() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("categoria").get("id"), filtro.categoriaId()));
            }
            if (filtro.fechaDesde() != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("fecha"), filtro.fechaDesde()));
            }
            if (filtro.fechaHasta() != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("fecha"), filtro.fechaHasta()));
            }
            return predicates;
        };
    }
}
